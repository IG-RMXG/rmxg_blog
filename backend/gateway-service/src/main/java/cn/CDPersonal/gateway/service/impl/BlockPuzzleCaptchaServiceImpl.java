package cn.CDPersonal.gateway.service.impl;

import cn.CDPersonal.gateway.config.CaptchaConfiguration;
import cn.CDPersonal.gateway.model.cache.CaptchaPointCache;
import cn.CDPersonal.gateway.model.dto.CaptchaDTO;
import cn.CDPersonal.gateway.model.params.CaptchaParam;
import cn.CDPersonal.gateway.model.vo.CaptchaVO;
import cn.CDPersonal.gateway.service.CaptchaService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * 拼图滑块验证码实现：不引第三方行为验证码 SDK，图片处理全部用 JDK 自带 API 完成。
 *
 * <p>生成流程：
 * <ol>
 *   <li>从配置的两个 classpath 目录各随机取一张图：底图 + 拼图模板（黑色部分为拼图块、其余为背景的蒙版）；</li>
 *   <li>在底图上随机取一个落点，把模板对应像素抠成拼图块，并把底图上的该区域做 3x3 均值模糊、加白色描边，
 *       使缺口在底图上可见；</li>
 *   <li>两张图编码为 PNG 后转 Base64 返回，答案坐标只写进 Redis。</li>
 * </ol>
 *
 * <p>校验约定：前端只回传拖动后的 x，服务端按 token 取答案，允许 ±2 px 误差；只比 x 不比 y，
 * 因为 y 由后端决定并已在 CaptchaVO 中下发。校验通过不会删除 token，TTL 内同一 token 可重复校验。
 *
 * <p>缓存：key 为随机 UUID（即下发的 token），value 为 {@link CaptchaPointCache}，TTL 5 分钟。
 *
 * <p>部署边界：图片目录是通过 {@code Resource#getFile()} 按文件系统枚举的，只在以展开目录运行
 * （IDE 运行 / target/classes）时可用；打成 jar 后需要改成读 jar 内条目才可用。
 */
@Service
public class BlockPuzzleCaptchaServiceImpl implements CaptchaService {

    private final CaptchaConfiguration properties;

    private final ResourceLoader resourceLoader;

    private final RedisTemplate<Object, Object> redisTemplate;

    //拼图块的坐标范围
    // 两个常量是落点相对左/上边缘的最小留白（px），同时决定了底图的最小可用尺寸，见 generateCaptcha
    private static final int BLOCK_X_RANGE = 100;
    private static final int BLOCK_Y_RANGE = 5;

    public BlockPuzzleCaptchaServiceImpl(CaptchaConfiguration properties, ResourceLoader resourceLoader, RedisTemplate<Object, Object> redisTemplate) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.redisTemplate = redisTemplate;
    }


    /**
     * 获取验证码
     *
     * @return 验证码
     */
    @Override
    public CaptchaVO getCaptcha() throws IOException {
        //获取原图
        String originPath = properties.getPaths().getOriginPath();
        BufferedImage original = getImages(originPath);
        if (null == original) {
            throw new RuntimeException("原图未初始化成功");
        }

        //获取预定义拼图模板
        String slidingBlockPath = properties.getPaths().getSlidingBlockPath();
        BufferedImage slidingBlock = getImages(slidingBlockPath);
        if (null == slidingBlock) {
            throw new RuntimeException("预定义拼图模板未初始化成功");
        }
        CaptchaDTO captcha = generateCaptcha(original, slidingBlock);
        if(captcha == null){
            throw new RuntimeException("验证码生成失败");
        }

        //对坐标点进行redis缓存
        String uuid = UUID.randomUUID().toString();
        CaptchaPointCache pointCache = new CaptchaPointCache(captcha.getX(), captcha.getY());
        redisTemplate.opsForValue().set(uuid, pointCache);
        // TTL 5 分钟：过期后该 token 的校验一律失败，前端需要重新获取验证码
        redisTemplate.expire(uuid, 5, TimeUnit.MINUTES);

        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setOriginalImageBase64(captcha.getOriginalImageBase64());
        captchaVO.setJigsawImageBase64(captcha.getJigsawImageBase64());
        captchaVO.setJigsawY(captcha.getY());
        captchaVO.setToken(uuid);
        return captchaVO;
    }

    /**
     * 校验滑动位置。
     *
     * <p>token 不存在（过期/伪造）、取出的值类型不符都返回 false 而不是抛异常，让上层统一按「验证失败」处理。
     * 取出的对象依赖 JSON 里的类型信息，所以这个 key 只能由本服务的 getCaptcha 写入。
     */
    public boolean checkCaptcha(CaptchaParam captchaParam){
        Object cachedObject = redisTemplate.opsForValue().get(captchaParam.getToken());
        if(cachedObject != null){
            try {
                CaptchaPointCache pointCache = (CaptchaPointCache) cachedObject;
                int tolerance = 2;
                return Math.abs(pointCache.getX() - captchaParam.getX()) <= tolerance;
            }catch (Exception e){
                return false;
            }
        }
        return false;
    }

    /**
     * 随机从资源文件夹中获取一种原始图片/预定义拼图模板
     *
     * @return 图片流
     */
    private BufferedImage getImages(String path) throws IOException {
        List<String> imagesAll = getImagesAll(path);
        if (imagesAll != null && !imagesAll.isEmpty()) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            String imageName = imagesAll.get(random.nextInt(imagesAll.size()));
            Resource resource = resourceLoader.getResource(path + "/" +imageName);
            try (InputStream inputStream = resource.getInputStream()) {
                return ImageIO.read(inputStream);
            }
        }
        return null;
    }

    /**
     * 获取图片该路径下的所有图片名集合
     *
     * @param path 文件路径地址
     * @return 文件名集合
     */
    private List<String> getImagesAll(String path) throws IOException {
        List<String> imageNameList = new ArrayList<>();
        Resource resource = resourceLoader.getResource(path);
        // 只支持「目录形式」的资源：打成 jar 后 classpath 资源不是文件，getFile() 会抛 FileNotFoundException
        File file = resource.getFile();
        // 检查文件是否存在且为目录
        if (!file.exists() || !file.isDirectory()) {
            return null;
        }

        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }

        for (File f : files) {
            if (f.isFile()) {
                String fileName = f.getName().toLowerCase();
                // 检查文件扩展名是否在允许的范围内
                if (properties.getAllowedExtensions().contains(getFileExtension(fileName))) {
                    imageNameList.add(fileName);
                }
            }
        }
        return imageNameList;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 生成验证码
     *
     * <p>底图相对拼图块过小时（宽/高不足）返回 null，由调用方转成「验证码生成失败」。
     * 注意本方法会就地修改传入的 original（抠图区域被模糊并描边），调用方不要复用同一个 BufferedImage。
     *
     * @param original       原图
     * @param slidingBlock 预定义拼图模板
     * @return 验证码
     */
    private CaptchaDTO generateCaptcha(BufferedImage original, BufferedImage slidingBlock) throws IOException {

        //获取原图的长宽
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        //预定义拼图块的宽高
        int blockWidth = slidingBlock.getWidth();
        int blockHeight = slidingBlock.getHeight();

        int widthSubtract = originalWidth - blockWidth;
        int heightSubtract = originalHeight - blockHeight;

        //如果原图尺寸过小，则返回null
        // 门槛与随机下界同值：widthSubtract-100 必须为正，否则 nextInt 会因参数非正抛 IllegalArgumentException
        if(widthSubtract <= 100 || heightSubtract <= 5){
            return null;
        }

        //生成随机坐标
        // 区间为 [留白, 原图尺寸-拼图块尺寸)，保证拼图块右侧与底部都还能取到像素
        int x = ThreadLocalRandom.current().nextInt(widthSubtract-BLOCK_X_RANGE)+BLOCK_X_RANGE;
        int y = ThreadLocalRandom.current().nextInt(heightSubtract-BLOCK_Y_RANGE)+BLOCK_Y_RANGE;

        //绘制一个和拼图块长宽一致的透明图块
        BufferedImage newBlockImage = new BufferedImage(blockWidth, blockHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newBlockImage.createGraphics();


        // 设置背景为透明
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, blockWidth, blockHeight);
        graphics.setComposite(AlphaComposite.SrcOver);

        //卷积核设定
        // 3x3 邻域均值（并非严格卷积）：对抠出的区域做一次轻度模糊降噪，让缺口边缘不那么生硬
        int[][] matrix = new int[3][3];
        int[] values = new int[9];

        for(int i = 0; i < blockWidth; ++i) {
            for(int j = 0; j < blockHeight; ++j) {
                int rgb = slidingBlock.getRGB(i, j);
                //预定义拼图块模板为黑色，所以这里判断小于0代表着属于拼图块，进行边界识别
                if (rgb < 0) {
                    //对新拼图块像素点赋值为原图对应像素点
                    newBlockImage.setRGB(i, j, original.getRGB(x + i, y + j));
                    //对原图进行平滑处理以及降噪
                    readPixel(original, x + i, y + j, values);
                    fillMatrix(matrix, values);
                    original.setRGB(x + i, y + j, avgMatrix(matrix));
                }
                //对拼图和原图对应位置进行白色描边处理
                // 只处理到倒数第二行/列：下面要读 i+1、j+1，最后一列/行越界会抛 ArrayIndexOutOfBounds
                if (i != blockWidth - 1 && j != blockHeight - 1) {
                    int rightRgb = slidingBlock.getRGB(i + 1, j);
                    int downRgb = slidingBlock.getRGB(i, j + 1);
                    if (rgb >= 0 && rightRgb < 0 || rgb < 0 && rightRgb >= 0 || rgb >= 0 && downRgb < 0 || rgb < 0 && downRgb >= 0) {
                        newBlockImage.setRGB(i, j, Color.white.getRGB());
                        original.setRGB(x + i, y + j, Color.white.getRGB());
                    }
                }
            }
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int bold = 5;
        graphics.setStroke(new BasicStroke((float)bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        graphics.drawImage(newBlockImage, 0, 0, (ImageObserver)null);
        graphics.dispose();

        //拼图的字节数组
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(newBlockImage, "png", os);
        byte[] newBlockImages = os.toByteArray();

        //原图字节数组
        ByteArrayOutputStream oriImagesOs = new ByteArrayOutputStream();
        ImageIO.write(original, "png", oriImagesOs);
        byte[] oriCopyImages = oriImagesOs.toByteArray();

        //转换为base64，返回验证码数据
        Base64.Encoder encoder = Base64.getEncoder();
        CaptchaDTO captcha = new CaptchaDTO();
        captcha.setOriginalImageBase64(encoder.encodeToString(oriCopyImages));
        captcha.setJigsawImageBase64(encoder.encodeToString(newBlockImages));
        captcha.setX(x);
        captcha.setY(y);
        return captcha;
    }

    /**
     * 读取像素
     *
     * @param img    图片
     * @param x      x坐标
     * @param y      y坐标
     * @param pixels 存储像素的数组
     */
    private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        // 取以 (x,y) 为中心的 3x3 邻域；越界时左/上侧按镜像取值，右/下侧退化为中心像素
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;

        for(int i = xStart; i < 3 + xStart; ++i) {
            for(int j = yStart; j < 3 + yStart; ++j) {
                int tx = i;
                if (i < 0) {
                    tx = -i;
                } else if (i >= img.getWidth()) {
                    tx = x;
                }

                int ty = j;
                if (j < 0) {
                    ty = -j;
                } else if (j >= img.getHeight()) {
                    ty = y;
                }

                pixels[current++] = img.getRGB(tx, ty);
            }
        }

    }

    /**
     * 填充矩阵
     *
     * @param matrix 矩阵
     * @param values 填充值
     */
    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;

        for(int i = 0; i < matrix.length; ++i) {
            int[] x = matrix[i];

            for(int j = 0; j < x.length; ++j) {
                x[j] = values[filled++];
            }
        }

    }

    /**
     * 计算矩阵的平均值
     *
     * @param matrix 矩阵
     * @return 平均值
     */
    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;

        for (int[] x : matrix) {
            for (int i : x) {
                Color c = new Color(i);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        // 计算平均值后降低亮度（乘以一个小于1的系数）
        // 实际实现是逐通道减 30（下限 0）而不是乘系数：缺口因此比周围暗一档，同时保留原图纹理
        int avgR = r / 9;
        int avgG = g / 9;
        int avgB = b / 9;

        // 加深颜色，降低亮度值
        avgR = Math.max(0, avgR - 30);  // 减少红色分量
        avgG = Math.max(0, avgG - 30);  // 减少绿色分量
        avgB = Math.max(0, avgB - 30);  // 减少蓝色分量

        return (new Color(avgR, avgG, avgB)).getRGB();
    }
}

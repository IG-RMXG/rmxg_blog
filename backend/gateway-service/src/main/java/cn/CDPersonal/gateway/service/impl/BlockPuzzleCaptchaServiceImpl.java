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


@Service
public class BlockPuzzleCaptchaServiceImpl implements CaptchaService {

    private final CaptchaConfiguration properties;

    private final ResourceLoader resourceLoader;

    private final RedisTemplate<Object, Object> redisTemplate;

    //拼图块的坐标范围
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
        redisTemplate.expire(uuid, 5, TimeUnit.MINUTES);

        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setOriginalImageBase64(captcha.getOriginalImageBase64());
        captchaVO.setJigsawImageBase64(captcha.getJigsawImageBase64());
        captchaVO.setJigsawY(captcha.getY());
        captchaVO.setToken(uuid);
        return captchaVO;
    }

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
        if(widthSubtract <= 100 || heightSubtract <= 5){
            return null;
        }

        //生成随机坐标
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

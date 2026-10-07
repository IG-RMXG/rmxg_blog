package cn.CDPersonal.dict.controller;

import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.dict.service.SysDictTypeService;
import cn.CDPersonal.dict.vo.ddictionary.DDictionaryReqVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/system/dict/type", method = RequestMethod.POST)
public class SysDictTypeController extends BaseController {

    @Autowired
    private SysDictTypeService sysDictTypeService;

    @ResponseBody
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public TableDataInfo index(DDictionaryReqVo ddictionaryReqVo) {
        startPage();
        return null;
    }

}

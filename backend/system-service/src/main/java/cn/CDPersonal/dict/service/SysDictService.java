package cn.CDPersonal.dict.service;

import cn.CDPersonal.common.domain.entity.SysDictData;
import cn.CDPersonal.common.domain.entity.SysDictType;
import cn.CDPersonal.dict.mapper.SysDictDataMapper;
import cn.CDPersonal.dict.mapper.SysDictTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典管理（字典类型 + 字典数据）。
 *
 * 两块业务合并在一个 service 里，因为它们耦合紧密：
 * 类型改名要同步数据里的 dict_type 冗余列，删类型前要检查是否还有数据。
 */
@Service
public class SysDictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;

    public SysDictService(SysDictTypeMapper typeMapper, SysDictDataMapper dataMapper) {
        this.typeMapper = typeMapper;
        this.dataMapper = dataMapper;
    }

    // ==================== 字典类型 ====================

    public List<SysDictType> selectTypeList(SysDictType query) {
        return typeMapper.selectDictTypeList(query);
    }

    public SysDictType selectTypeById(Long dictId) {
        return typeMapper.selectDictTypeById(dictId);
    }

    public List<SysDictType> selectTypeAll() {
        return typeMapper.selectDictTypeAll();
    }

    /** 字典类型是否唯一（新增时 dictId 为空，修改时排除自身） */
    public boolean checkTypeUnique(SysDictType dictType) {
        SysDictType exist = typeMapper.checkDictTypeUnique(dictType.getDictType());
        if (exist == null) {
            return true;
        }
        return exist.getDictId() != null && exist.getDictId().equals(dictType.getDictId());
    }

    @Transactional
    public int insertType(SysDictType dictType) {
        return typeMapper.insertDictType(dictType);
    }

    /** 修改类型；若 dict_type 变了，同步更新字典数据里的冗余值 */
    @Transactional
    public int updateType(SysDictType dictType) {
        SysDictType old = typeMapper.selectDictTypeById(dictType.getDictId());
        if (old != null && old.getDictType() != null
                && !old.getDictType().equals(dictType.getDictType())) {
            typeMapper.updateDictDataType(old.getDictType(), dictType.getDictType());
        }
        return typeMapper.updateDictType(dictType);
    }

    /** 删除类型前先检查是否被字典数据引用 */
    public boolean hasDataType(Long dictId) {
        SysDictType type = typeMapper.selectDictTypeById(dictId);
        return type != null && dataMapper.countDictDataByType(type.getDictType()) > 0;
    }

    @Transactional
    public int deleteTypeByIds(Long[] dictIds) {
        return typeMapper.deleteDictTypeByIds(dictIds);
    }

    // ==================== 字典数据 ====================

    public List<SysDictData> selectDataList(SysDictData query) {
        return dataMapper.selectDictDataList(query);
    }

    public SysDictData selectDataById(Long dictCode) {
        return dataMapper.selectDictDataById(dictCode);
    }

    public List<SysDictData> selectDataByType(String dictType) {
        return dataMapper.selectDictDataByType(dictType);
    }

    @Transactional
    public int insertData(SysDictData dictData) {
        return dataMapper.insertDictData(dictData);
    }

    @Transactional
    public int updateData(SysDictData dictData) {
        return dataMapper.updateDictData(dictData);
    }

    @Transactional
    public int deleteDataByIds(Long[] dictCodes) {
        return dataMapper.deleteDictDataByIds(dictCodes);
    }
}

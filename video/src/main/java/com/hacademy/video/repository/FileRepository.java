package com.hacademy.video.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.vo.FileListVO;

@Repository
public class FileRepository {
	
	@Autowired
	private FileProperties props;

	public List<FileListVO> selectList() {
		List<FileListVO> list = new ArrayList<>();
		FileListVO vo = new FileListVO();
		vo.setNo(1);
		vo.setName("");
		list.add(vo);
		return list;
	}
	
	public FileListVO selectOne(int directory) {
		for(FileListVO vo : selectList()) {
			if(vo.getNo() == directory)
				return vo;
		}
		return null;
	}
//	
//	public List<FileItemVO> selectSubList(int directory) {
//		
//	}
	
}

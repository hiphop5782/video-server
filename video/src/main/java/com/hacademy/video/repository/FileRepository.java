package com.hacademy.video.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.vo.FileItemVO;
import com.hacademy.video.vo.FileListVO;

@Repository
public class FileRepository {
	
	@Autowired
	private FileProperties props;

	public List<FileListVO> selectList() {
		List<FileListVO> list = new ArrayList<>();
		list.add(FileListVO.builder().no(1).name("kh11").build());
		return list;
	}
	
//	public FileListVO selectOne(int directory) {
//		
//	}
//	
//	public List<FileItemVO> selectSubList(int directory) {
//		
//	}
	
}

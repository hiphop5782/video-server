package com.hacademy.video.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FileItemVO {
	String filename;
	long filesize;
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public long getFilesize() {
		return filesize;
	}
	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}
}

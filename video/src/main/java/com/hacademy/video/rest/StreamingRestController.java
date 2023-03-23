package com.hacademy.video.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.repository.FileRepository;
import com.hacademy.video.vo.FileListVO;

@CrossOrigin
@RestController
public class StreamingRestController {
	
	@Autowired
	private FileProperties props;
	
	@Autowired
	private FileRepository repo;
	
	@GetMapping("/play")
	public List<FileListVO> play() {
		return repo.selectList();
	}
	
	@GetMapping("/play/{directory}")
	public String[] directory(@PathVariable int directory) {
		File target = new File(props.getPath(), getDirectory(directory));
		if(target.isDirectory())
			return target.list();
		return null;
	}

	@GetMapping("/play/{directory}/{filename:.+}")
	public ResponseEntity<ResourceRegion> playVideo(
			@RequestHeader HttpHeaders headers,
			@PathVariable int directory,
			@PathVariable String filename) throws IOException {
		File dir = new File(props.getPath(), getDirectory(directory));
		File target = new File(dir, filename);
		Resource resource = new FileSystemResource(target);	
		ResourceRegion region = createResourceRegion(resource, headers);
		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
										.contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
										.body(region);
	}
	
	private String getDirectory(int directory) {
		return switch(directory) {
		case 1 -> "kh11";
		default -> "";
		};
	}
	
	private ResourceRegion createResourceRegion(
				Resource resource, HttpHeaders headers) throws IOException {
		final long chunkSize = 100000L;
		long contentLength = resource.contentLength();
		
		HttpRange range = headers.getRange().stream().findFirst().orElse(null);
		if(range != null) {
			long start = range.getRangeStart(contentLength);
			long end = range.getRangeEnd(contentLength);
			long rangeLength = Long.min(chunkSize, end-start+1);
			return new ResourceRegion(resource, start, rangeLength);
		}
		else {
			long rangeLength = Long.min(chunkSize, contentLength);
			return new ResourceRegion(resource, 0, rangeLength);
		}
	}
	
}
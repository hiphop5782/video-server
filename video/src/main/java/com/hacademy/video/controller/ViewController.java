package com.hacademy.video.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

//@Controller
public class ViewController {

	@RequestMapping("/")
	public String view() {
		return "/WEB-INF/views/home.jsp";
	}
	
}

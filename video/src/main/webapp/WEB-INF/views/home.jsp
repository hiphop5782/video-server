<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Video Player</title>
	<link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/static/favicon.ico">
    <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.3.0/css/all.min.css">
    
	<link rel="stylesheet" type="text/css" href="https://vjs.zencdn.net/8.0.4/video-js.css">
	<link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/bootswatch/5.2.3/cerulean/bootstrap.min.css">
	<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/static/css/sidebars.css">
	<style>
		body {
			overflow-y:scroll;
		}
		.sidebar {
			--bs-gutter-y:1.5rem;
			width:250px;
			padding-top:100px;
		}
		.sidebar-header {
			--bs-gutter-x:1.5rem;
			--bs-gutter-y:1.5rem;
			position:fixed;
			z-index:999999;
			top:0;
			left:calc(var(--bs-gutter-x) * .5);
			width:250px;
			background-color:var(--bs-white);
		}
		.list-group-item {
			cursor:pointer;
			overflow:hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
		.video-container {
			--bs-gutter-x:1.5rem;
			--bs-gutter-y:1.5rem;
			position: fixed;
			top:calc(var(--bs-gutter-y) * .5);
			right:calc(var(--bs-gutter-x) * .5);
			bottom:calc(var(--bs-gutter-y) * .5);
			left:calc(var(--bs-gutter-x) + 250px);
		}
		.video-container .video-player {
			width:100%;
			height:100%;
		}
	</style>
</head>
<body>
	<div id="app" class="container-fluid">
		<div class="d-flex flex-column align-items-stretch flex-shrink-0 bg-white sidebar">
		    <div class="sidebar-header">
			    <a href="${pageContext.request.contextPath}" class="d-flex align-items-center flex-shrink-0 p-3 link-dark text-decoration-none border-bottom">
			    	<img class="bi me-2" width="30" height="24" src="${pageContext.request.contextPath}/static/favicon.ico">
			      	<span class="fs-5 fw-semibold">Video Player</span>
			    </a>
			    <div class="row">
			    	<div class="col pt-1 pb-1 pe-4">
			    		<input type="text" class="form-control" placeholder="검색어 입력" autocomplete="off" v-model="keyword" @input="keyword = $event.target.value">
			    	</div>
			    </div>
		    </div>
<!-- 		    <div class="list-group list-group-flush border-bottom scrollarea" v-for="(video, index) in videos" :key="index"> -->
			<div class="list-group list-group-flush border-bottom scrollarea" v-for="(video, index) in searchResults" :key="index">
				<div class="list-group-item list-group-item-action py-3 lh-tight" @click="selectVideo(index)" :class="{active:currentVideo == searchResults[index]}">
					{{video}}
		      	</div>
		    </div>
		</div>
		<div class="video-container">
			<video
			    class="video-js vjs-big-play-centerd vjs-fluid video-player"
			    id="video-player"
			    poster="${pageContext.request.contextPath}/static/image/hacademy.png"
			  ></video>
			<div class="row mt-4" v-if="currentVideo != null">
				<div class="col">
					<h2>
						{{currentVideo}}
<!-- 						({{position}}/{{duration}}) -->
					</h2>
					<button type="button" class="btn btn-outline-secondary me-2" @click="ss">처음</button>
					<button type="button" class="btn btn-outline-secondary me-2" @click="rr(10)">-10s</button>
					<button type="button" class="btn btn-outline-secondary me-2" @click="rr(5)">-5s</button>
					<button type="button" class="btn btn-outline-secondary me-2" @click="ff(5)">+5s</button>
					<button type="button" class="btn btn-outline-secondary me-2" @click="ff(10)">+10s</button>
				</div>
			</div>
		</div>
	</div>
	
	<script>
		const context = "${pageContext.request.contextPath}";
	</script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/lodash.js/4.17.21/lodash.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.4/moment.min.js"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.2.3/js/bootstrap.bundle.min.js"></script>
	<script src="https://unpkg.com/vue@3"></script>
	<script src="https://vjs.zencdn.net/8.0.4/video.min.js"></script>
	<script src="${pageContext.request.contextPath}/static/js/sidebars.js"></script>
	<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
</body>
</html>
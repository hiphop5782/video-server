Vue.createApp({
	data(){
		return {
			videos:[],
			currentVideo:null,
			player:null,
			duration:null,
			keyword:"",
		};
	},
	computed:{
		searchResults() {
			if(this.keyword.length == 0) return this.videos;
			var keyword = this.keyword.toLowerCase();
			return this.videos.filter(video=>video.toLowerCase().indexOf(keyword) >= 0);
		},
	},
	methods:{
		async loadVideos() {
			const response = await axios.get(context+"/play/1");
			this.videos = response.data;
		},
		selectVideo(index) {
			this.currentVideo = this.searchResults[index];
		},
		ss() {
			this.player.currentTime(0);
		},
		ff(value){
			this.player.currentTime(this.player.currentTime() + value);
		},
		rr(value){
			this.player.currentTime(this.player.currentTime() - value);
		},
		keyHandler:_.throttle(function(e){
			if(this.currentVideo == null) return;
			
			switch(e.keyCode) {
				case 37: this.rr(10); break;
				case 39: this.ff(10); break;
			};
		}, 100),
	},
	watch:{
		currentVideo(value){
			this.player.src({type:'video/mp4', src:context+`/play/1/${value}`});
			this.player.play();
			this.player.one("loadedmetadata", ()=>{
				this.duration = this.player.duration();
			});
		},
	},
	created(){
		this.loadVideos();
	},
	mounted(){
		this.player = videojs("video-player", {
			controls:true,
			autoplay:false,
			loop:false,
			preload:"auto",
			fill:true,
		});
		window.addEventListener("keydown", this.keyHandler);
	},
	updated(){}
}).mount("#app");
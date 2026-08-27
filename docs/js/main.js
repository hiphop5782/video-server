
/*
if(location.protocol == 'https:') {
	window.alert('https로 접속하시면 영상이 나오지 않으니 참고 바랍니다');
	//location.href = location.href.replace('https', 'http');
	const url = "http" + location.href.substring('https'.length);
	location.href = url;
}
*/


import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-app.js'

// If you enabled Analytics in your project, add the Firebase SDK for Google Analytics
import { getAnalytics } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-analytics.js'

// Add Firebase products that you want to use
import { GithubAuthProvider, browserLocalPersistence, getAuth, onAuthStateChanged, setPersistence, signInWithPopup, signOut } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-auth.js'
import { getFirestore } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-firestore.js'


// const context = "http://localhost:30000";
//const context = "http://khds-c.iptime.org:30000";
const context = "//video-api.sysout.co.kr:30000";
Vue.createApp({
	data(){
		return {
			videos:[],
			currentVideo:null,
			player:null,
			duration:null,
			keyword:"",
			provider:null,
			auth:null,
			user:null,
			token:null,
			authReady:false,
			loading:false,
			errorMessage:"",
			playbackRate:1,
			listScrollTop:0,
			listViewportHeight:600,
			listItemHeight:52,
			progressByVideo:{},
			currentProgress:0,
			autoPlayNext:localStorage.getItem('video-player:auto-next') !== 'false',
		};
	},
	computed:{
		searchResults() {
			const terms = this.normalizeSearch(this.keyword).split(' ').filter(Boolean);
			if(!terms.length) return this.videos;
			return this.videos.filter(video=>{
				const target = this.normalizeSearch(this.displayName(video));
				return terms.every(term=>target.includes(term));
			});
		},
		virtualTotalHeight(){ return this.searchResults.length * this.listItemHeight; },
		virtualRows(){
			const overscan = 5;
			const start = Math.max(0, Math.floor(this.listScrollTop / this.listItemHeight) - overscan);
			const count = Math.ceil(this.listViewportHeight / this.listItemHeight) + overscan * 2;
			return this.searchResults.slice(start,start+count).map((video,index)=>({video,top:(start+index)*this.listItemHeight}));
		},
		nextVideo(){
			const index=this.videos.indexOf(this.currentVideo);
			return index>=0 && index<this.videos.length-1 ? this.videos[index+1] : null;
		},
	},
	methods:{
		normalizeSearch(value){ return String(value||'').normalize('NFKC').toLocaleLowerCase('ko-KR').replace(/[_\-.]+/g,' ').replace(/\s+/g,' ').trim(); },
		handleListScroll(event){ this.listScrollTop=event.target.scrollTop; this.listViewportHeight=event.target.clientHeight; },
		resetListScroll(){ this.listScrollTop=0; this.$nextTick(()=>{ if(this.$refs.virtualList) this.$refs.virtualList.scrollTop=0; }); },
		selectVideo(video) {
			this.currentVideo = video;
			localStorage.setItem('video-player:last-video', video);
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
		speed(value) {
			this.playbackRate = value;
			this.player.playbackRate(value);
		},
		keyHandler:_.throttle(function(e){
			if(this.currentVideo == null) return;
			
			switch(e.keyCode) {
				case 37: this.rr(10); break;
				case 39: this.ff(10); break;
			};
		}, 100),
		async githubSignIn(){
			this.loading = true;
			this.errorMessage = '';
			try {
				await setPersistence(this.auth, browserLocalPersistence);
				await signInWithPopup(this.auth, this.provider);
			} catch(error) {
				if(error.code !== 'auth/popup-closed-by-user') this.errorMessage = '로그인하지 못했습니다. 다시 시도해 주세요.';
			} finally { this.loading = false; }
		},
		async loadVideos(){
			if(!this.user) return;
			this.loading = true;
			this.errorMessage = '';
			try {
			const response2 = await axios.get(`${context}/data`, {
				headers:{
					user:this.user.providerData[0]?.uid || this.user.uid
				}
			});
			this.videos = response2.data;
			this.token = response2.headers["token"];
			this.loadSavedProgress();
			const lastVideo = localStorage.getItem('video-player:last-video');
			if(this.videos.includes(lastVideo)) this.currentVideo = lastVideo;
			} catch(error) {
				this.errorMessage = '영상 목록을 불러오지 못했습니다.';
			} finally { this.loading = false; }
		},
		async logout(){ await signOut(this.auth); },
		displayName(video){
			return String(video || '')
				.replace(/\.[^.]+$/, '')
				.replace(/(^|[^\d])(?:19|20)\d{2}[._-]?(?:0[1-9]|1[0-2])[._-]?(?:0[1-9]|[12]\d|3[01])(?=$|[^\d])/g, '$1')
				.replace(/(?:19|20)\d{2}년\s*(?:0?[1-9]|1[0-2])월\s*(?:0?[1-9]|[12]\d|3[01])일/g, '')
				.replace(/\(\s*\)|\[\s*\]/g, '')
				.replace(/^[\s._-]+|[\s._-]+$/g, '')
				.replace(/\s{2,}/g, ' ');
		},
		clearSearch(){ this.keyword = ''; this.$refs.searchInput?.focus(); },
		savePosition(){
			if(!this.player || !this.currentVideo) return;
			const position=this.player.currentTime(), duration=this.player.duration();
			if(!Number.isFinite(position) || !Number.isFinite(duration) || duration<=0) return;
			const progress={position,duration};
			this.progressByVideo[this.currentVideo]=progress;
			this.currentProgress=Math.min(100,Math.round(position/duration*100));
			localStorage.setItem(`video-player:progress:${this.currentVideo}`,JSON.stringify(progress));
		},
		progressPercent(video){ const value=this.progressByVideo[video]; return value?.duration ? Math.min(100,Math.round(value.position/value.duration*100)) : 0; },
		loadSavedProgress(){
			const progress={};
			this.videos.forEach(video=>{ try { const value=JSON.parse(localStorage.getItem(`video-player:progress:${video}`)); if(value?.duration) progress[video]=value; } catch(error){} });
			this.progressByVideo=progress;
		},
		playNext(){ if(this.nextVideo) this.selectVideo(this.nextVideo); },

	},
	watch:{
		keyword(){ this.resetListScroll(); },
		autoPlayNext(value){ localStorage.setItem('video-player:auto-next',String(value)); },
		currentVideo(value){
			if(!value || !this.token) return;
			this.currentProgress=this.progressPercent(value);
			this.errorMessage='';
			this.player.src({type:'video/mp4', src:`${context}/play/${encodeURIComponent(this.token)}/${encodeURIComponent(value)}`});
			this.player.one("loadedmetadata", ()=>{
				this.duration = this.player.duration();
				const saved = this.progressByVideo[value]?.position || Number(localStorage.getItem(`video-player:position:${value}`));
				if(saved > 0 && saved < this.duration - 5) this.player.currentTime(saved);
				this.player.play().catch(()=>{});
			});
		},
	},
	created(){
		const firebaseConfig = {
			apiKey: "AIzaSyBMVq-lq8w-YHP7X8gd6kU3lh0g2mf-4qo",
			authDomain: "hacademy-3a057.firebaseapp.com",
			projectId: "hacademy-3a057",
			storageBucket: "hacademy-3a057.appspot.com",
			messagingSenderId: "661336156675",
			appId: "1:661336156675:web:3b2b75f429e5c6b8f3fc36",
			measurementId: "G-DXQ1WQ5DL2"
		};

		const app = initializeApp(firebaseConfig);
		const analytics = getAnalytics(app);

		this.provider = new GithubAuthProvider();
		this.auth = getAuth();
		setPersistence(this.auth, browserLocalPersistence).catch(console.warn);
		onAuthStateChanged(this.auth, async user=>{
			this.user = user;
			this.authReady = true;
			if(user) await this.loadVideos();
			else { this.videos=[]; this.currentVideo=null; this.token=null; if(this.player) this.player.reset(); }
		});
	},
	mounted(){
		this.player = videojs("video-player", {
			controls:true,
			autoplay:false,
			loop:false,
			preload:"auto",
			fill:true,
			playbackRates:[1,1.2,1.5,2]
		});
		window.addEventListener("keydown", this.keyHandler);
		this.player.on('timeupdate', _.throttle(this.savePosition, 3000));
		this.player.on('ended', ()=>{ this.savePosition(); if(this.autoPlayNext) this.playNext(); });
		this.player.on('error', ()=>{ this.errorMessage='영상을 재생할 수 없습니다. 파일 상태를 확인하거나 다른 영상을 선택해 주세요.'; });
		window.addEventListener('beforeunload', this.savePosition);
	},
	updated(){}
}).mount("#app");

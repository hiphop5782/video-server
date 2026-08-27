
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
		};
	},
	computed:{
		searchResults() {
			if(this.keyword.length == 0) return this.videos;
			const keyword = this.keyword.normalize('NFKC').toLocaleLowerCase('ko-KR').replace(/\s+/g, ' ').trim();
			return this.videos.filter(video=>video.normalize('NFKC').toLocaleLowerCase('ko-KR').includes(keyword));
		},
	},
	methods:{
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
			const lastVideo = localStorage.getItem('video-player:last-video');
			if(this.videos.includes(lastVideo)) this.currentVideo = lastVideo;
			} catch(error) {
				this.errorMessage = '영상 목록을 불러오지 못했습니다.';
			} finally { this.loading = false; }
		},
		async logout(){ await signOut(this.auth); },
		displayName(video){ return String(video || '').replace(/\.[^.]+$/, ''); },
		clearSearch(){ this.keyword = ''; this.$refs.searchInput?.focus(); },
		savePosition(){
			if(this.player && this.currentVideo) localStorage.setItem(`video-player:position:${this.currentVideo}`, this.player.currentTime());
		},

	},
	watch:{
		currentVideo(value){
			if(!value || !this.token) return;
			this.player.src({type:'video/mp4', src:`${context}/play/${encodeURIComponent(this.token)}/${encodeURIComponent(value)}`});
			this.player.one("loadedmetadata", ()=>{
				this.duration = this.player.duration();
				const saved = Number(localStorage.getItem(`video-player:position:${value}`));
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
		window.addEventListener('beforeunload', this.savePosition);
	},
	updated(){}
}).mount("#app");

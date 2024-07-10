
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
import { GithubAuthProvider, getAuth, signInWithPopup } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-auth.js'
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
		speed(value) {
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
			const response = await signInWithPopup(this.auth, this.provider);
			//console.log(response.user.reloadUserInfo.screenName);
			//console.log(response.user);
			this.user = response.user;

			const response2 = await axios.get(`${context}/data`, {
				headers:{
					//user:response.user.reloadUserInfo.screenName
					user:response.user.providerData[0].uid
				}
			});
			this.videos = response2.data;
			this.token = response2.headers["token"];
		},

	},
	watch:{
		currentVideo(value){
			this.player.src({type:'video/mp4', src:`${context}/play/${this.token}/${value}`});
			this.player.play();
			this.player.one("loadedmetadata", ()=>{
				this.duration = this.player.duration();
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
	},
	updated(){}
}).mount("#app");

import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-app.js'
import { getAnalytics } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-analytics.js'
import { GithubAuthProvider, browserLocalPersistence, getAuth, onAuthStateChanged, setPersistence, signInWithPopup, signOut } from 'https://www.gstatic.com/firebasejs/9.22.2/firebase-auth.js'

const context = "https://video-api.sysout.co.kr";
axios.defaults.withCredentials = true;

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
			sessionType:null,
			accessCode:"",
			authReady:false,
			loading:false,
			errorMessage:"",
			playbackRate:1,
			listScrollTop:0,
			listViewportHeight:600,
			listItemHeight:52,
			progressByVideo:{},
			currentProgress:0,
			isPlaying:false,
			autoPlayNext:localStorage.getItem('video-player:auto-next') !== 'false',
			adminOpen:false,
			adminAuthenticated:false,
			adminPassword:"",
			adminLoading:false,
			adminError:"",
			adminNotice:"",
			adminRepositories:[],
			adminGrants:[],
			grantForm:{code:"",repository:"",description:"",expiresAt:""},
		};
	},
	computed:{
		signedIn(){ return this.sessionType !== null; },
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
		ss(){ this.player.currentTime(0); },
		ff(value){ this.player.currentTime(this.player.currentTime() + value); },
		rr(value){ this.player.currentTime(this.player.currentTime() - value); },
		speed(value) {
			this.playbackRate = value;
			this.player.playbackRate(value);
		},
		keyHandler:_.throttle(function(e){
			if(this.currentVideo == null) return;
			switch(e.keyCode) {
				case 37: this.rr(10); break;
				case 39: this.ff(10); break;
			}
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
		async establishGithubSession(user){
			this.loading = true;
			this.errorMessage = '';
			try {
				const idToken = await user.getIdToken();
				await axios.post(`${context}/api/auth/github`, {}, {headers:{Authorization:`Bearer ${idToken}`}});
				this.sessionType = 'github';
				await this.loadVideos();
			} catch(error) {
				this.clearViewer();
				this.errorMessage = this.apiError(error, 'GitHub 접근 권한을 확인하지 못했습니다.');
			} finally { this.loading = false; }
		},
		async accessCodeSignIn(){
			if(this.accessCode.length < 6) return;
			this.loading = true;
			this.errorMessage = '';
			try {
				await axios.post(`${context}/api/access/login`, {code:this.accessCode});
				this.sessionType = 'access-code';
				this.accessCode = '';
				await this.loadVideos();
			} catch(error) {
				this.clearViewer();
				this.errorMessage = this.apiError(error, '접근 코드가 올바르지 않거나 만료되었습니다.');
			} finally { this.loading = false; }
		},
		async restoreSession(){
			try {
				const response = await axios.get(`${context}/api/videos`);
				this.sessionType = 'access-code';
				this.applyVideos(response.data);
			} catch(error) {
				if(error.response?.status !== 401) this.errorMessage = '기존 로그인 상태를 확인하지 못했습니다.';
			}
		},
		async loadVideos(){
			if(!this.signedIn) return;
			this.loading = true;
			this.errorMessage = '';
			try {
				const response = await axios.get(`${context}/api/videos`);
				this.applyVideos(response.data);
			} catch(error) {
				if(error.response?.status === 401) this.clearViewer();
				this.errorMessage = this.apiError(error, '영상 목록을 불러오지 못했습니다.');
			} finally { this.loading = false; }
		},
		applyVideos(videos){
			this.videos = Array.isArray(videos) ? videos : [];
			this.loadSavedProgress();
			const lastVideo = localStorage.getItem('video-player:last-video');
			if(this.videos.includes(lastVideo)) this.currentVideo = lastVideo;
		},
		async logout(){
			try { await axios.post(`${context}/api/logout`); } catch(error){}
			if(this.user) await signOut(this.auth);
			this.clearViewer();
		},
		clearViewer(){
			this.sessionType=null;
			this.videos=[];
			this.currentVideo=null;
			this.isPlaying=false;
			if(this.player) this.player.reset();
		},
		displayName(video){
			return String(video || '')
				.replace(/\.[^.]+$/, '')
				.replace(/(^|[^\d])(?:19|20)\d{2}[._-]?(?:0[1-9]|1[0-2])[._-]?(?:0[1-9]|[12]\d|3[01])(?=$|[^\d])/g, '$1')
				.replace(/(?:19|20)\d{2}년\s*(?:0?[1-9]|1[0-2])월\s*(?:0?[1-9]|[12]\d|3[01])일/g, '')
				.replace(/^\s*[\[(]?\s*(?:[01]\d|2[0-3])(?:[:._-]?[0-5]\d){1,2}\s*[\])]?[\s._-]*/, '')
				.replace(/\(\s*\)|\[\s*\]/g, '')
				.replace(/^[\s._-]+|[\s._-]+$/g, '')
				.replace(/\s{2,}/g, ' ');
		},
		detailFileName(video){ return String(video || '').replace(/\.[^.]+$/, ''); },
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
		openAdmin(){
			this.adminOpen=true;
			this.adminError='';
			this.adminNotice='';
			this.loadAdminData();
		},
		closeAdmin(){ this.adminOpen=false; },
		async adminLogin(){
			this.adminLoading=true;
			this.adminError='';
			try {
				await axios.post(`${context}/api/admin/login`, {password:this.adminPassword});
				this.adminAuthenticated=true;
				this.adminPassword='';
				await this.loadAdminData();
			} catch(error) {
				this.adminAuthenticated=false;
				this.adminError=this.apiError(error,'관리자 로그인에 실패했습니다.');
			} finally { this.adminLoading=false; }
		},
		async adminLogout(){
			try { await axios.post(`${context}/api/admin/logout`); } catch(error){}
			this.adminAuthenticated=false;
			this.adminGrants=[];
			this.adminRepositories=[];
		},
		async loadAdminData(){
			this.adminLoading=true;
			this.adminError='';
			try {
				const [grants,repositories] = await Promise.all([
					axios.get(`${context}/api/admin/grants`),
					axios.get(`${context}/api/admin/repositories`)
				]);
				this.adminAuthenticated=true;
				this.adminGrants=grants.data;
				this.adminRepositories=repositories.data;
				if(!this.grantForm.repository && this.adminRepositories.length) this.grantForm.repository=this.adminRepositories[0];
			} catch(error) {
				if(error.response?.status === 401) this.adminAuthenticated=false;
				else this.adminError=this.apiError(error,'접근 권한을 불러오지 못했습니다.');
			} finally { this.adminLoading=false; }
		},
		generateCode(){
			const alphabet='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
			const bytes=new Uint32Array(8);
			crypto.getRandomValues(bytes);
			const value=Array.from(bytes,n=>alphabet[n%alphabet.length]).join('');
			this.grantForm.code=`TEMP-${value.slice(0,4)}-${value.slice(4)}`;
		},
		async createGrant(){
			this.adminLoading=true;
			this.adminError='';
			this.adminNotice='';
			const code=this.grantForm.code;
			try {
				const expiresAt=this.grantForm.expiresAt ? new Date(this.grantForm.expiresAt).toISOString() : null;
				await axios.post(`${context}/api/admin/grants`, {
					code,
					repository:this.grantForm.repository,
					description:this.grantForm.description,
					expiresAt
				});
				this.adminNotice=`접근 코드 ${code} 권한을 생성했습니다. 지금 복사해 전달하세요.`;
				this.grantForm={code:'',repository:this.grantForm.repository,description:'',expiresAt:''};
				await this.loadAdminData();
			} catch(error) {
				this.adminError=this.apiError(error,'접근 권한을 생성하지 못했습니다.');
			} finally { this.adminLoading=false; }
		},
		async deleteGrant(grant){
			if(!window.confirm(`${grant.repository} 접근 권한을 삭제할까요? 현재 시청 세션도 즉시 종료됩니다.`)) return;
			this.adminError='';
			try {
				await axios.delete(`${context}/api/admin/grants/${grant.id}`);
				this.adminGrants=this.adminGrants.filter(item=>item.id!==grant.id);
			} catch(error) {
				this.adminError=this.apiError(error,'접근 권한을 삭제하지 못했습니다.');
			}
		},
		formatDate(value){ return value ? new Intl.DateTimeFormat('ko-KR',{dateStyle:'medium',timeStyle:'short'}).format(new Date(value)) : '-'; },
		apiError(error,fallback){ return error.response?.data?.message || fallback; },
	},
	watch:{
		keyword(){ this.resetListScroll(); },
		autoPlayNext(value){ localStorage.setItem('video-player:auto-next',String(value)); },
		currentVideo(value){
			if(!value || !this.signedIn) return;
			this.isPlaying=false;
			this.currentProgress=this.progressPercent(value);
			this.errorMessage='';
			this.player.src({type:'video/mp4', src:`${context}/api/videos/${encodeURIComponent(value)}`});
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
		getAnalytics(app);
		this.provider = new GithubAuthProvider();
		this.auth = getAuth();
		setPersistence(this.auth, browserLocalPersistence).catch(console.warn);
		onAuthStateChanged(this.auth, async user=>{
			this.user = user;
			this.authReady = true;
			if(user) await this.establishGithubSession(user);
			else if(!this.sessionType) await this.restoreSession();
		});
	},
	mounted(){
		this.player = videojs("video-player", {
			controls:true,
			autoplay:false,
			loop:false,
			preload:"auto",
			fluid:true,
			aspectRatio:"16:9",
			playbackRates:[1,1.2,1.5,2]
		});
		window.addEventListener("keydown", this.keyHandler);
		this.player.on('play', ()=>{ this.isPlaying=true; });
		this.player.on('pause', ()=>{ this.isPlaying=false; });
		this.player.on('timeupdate', _.throttle(this.savePosition, 3000));
		this.player.on('ended', ()=>{ this.isPlaying=false; this.savePosition(); if(this.autoPlayNext) this.playNext(); });
		this.player.on('error', ()=>{ this.isPlaying=false; this.errorMessage='영상을 재생할 수 없습니다. 파일 상태를 확인하거나 다른 영상을 선택해 주세요.'; });
		window.addEventListener('beforeunload', this.savePosition);
	}
}).mount("#app");

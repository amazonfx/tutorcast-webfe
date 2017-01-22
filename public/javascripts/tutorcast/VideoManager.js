var global_video_holder = {};
var VideoManager = Class.create({
    initialize: function(videoContainer, board) {
        this._board = board;
        this._apiKey = 17327571;
        this._sessionId = GlobalConstants.tokSession;
        this._token = GlobalConstants.tokToken;
       
        this._instantiateTime = new Date().getTime();
        this._instantiateOffset = -1;

        this._videoContainer = document.getElementById(videoContainer);
        this._teacherContainer = document.getElementById("teacherContainer");
        this._studentContainer = document.getElementById("studentContainer");
        this._archiveContainer = document.getElementById("archiveContainer");
        
        this._publisher = {};
        //TB.setLogLevel(TB.DEBUG);  
        
        this.SCREEN_WIDTH = 280;
        this.SCREEN_HEIGHT = 215;
        
        this.ARCHIVE_SCREEN_WIDTH = 280;
        this.ARCHIVE_SCREEN_HEIGHT= 213;
        this._studentArchiveVisible = 0;
        this._archivesResized = false;
        
        if (!GlobalConstants.replayMode){ 
        	this._teacherContainer.style.width = this.SCREEN_WIDTH+"px";
            this._teacherContainer.style.height =  this.SCREEN_HEIGHT+"px";
            this._studentContainer.style.width = this.SCREEN_WIDTH+"px";
        	this._studentContainer.style.height =  this.SCREEN_HEIGHT+"px";
            this._session = TB.initSession(this._sessionId);
        	this.registerListeners(this);
        	OT_LayoutContainer.init("studentContainer", this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
            this._session.connect(this._apiKey, this._token);
            
        } else {
        	this._teacherContainer.style.width = this.ARCHIVE_SCREEN_WIDTH+"px";
            this._teacherContainer.style.height =  this.ARCHIVE_SCREEN_HEIGHT+"px";
            this._studentContainer.style.width = this.ARCHIVE_SCREEN_WIDTH+"px";
        	this._studentContainer.style.height =  this.ARCHIVE_SCREEN_HEIGHT+"px";
        }
        
        this._archive = null;
        this._archiveName = GlobalConstants.roomName;

        this._archiveData = null;
        this._replayPlayers = {};

        this._desiredPos = {};
        this._seekFlag = {};
        this._loadArchivedPromise = null;
        this._seekPlayPromise = null;

        global_video_holder = this;
        this._firstClipStart = 0;
        this._totalDuration = 0;
        this._verifyPlayingList = {};
        this._lastSecond = false;
        
		
    },

    registerListeners: function(self) {
        self._session.addEventListener('sessionConnected', function(e) {
            if (!GlobalConstants.replayMode) {
            	var divId = 'tokPublish';
            	var screenSize = {};
            	if (GlobalConstants.userID == GlobalConstants.teacherID) {
            		self._publishDiv = document.createElement('div');
                    self._publishDiv.id = divId;
                	self._teacherContainer.appendChild(self._publishDiv);
                	screenSize = {width: self.SCREEN_WIDTH, height: self.SCREEN_HEIGHT, name: "Instructor" };
                } else {
                	screenSize = {width: self.SCREEN_WIDTH, height: self.SCREEN_HEIGHT, name: GlobalConstants.firstName };
                	OT_LayoutContainer.addStream(divId, true);
                	OT_LayoutContainer.layout();
                }
            	
                self._publisher = TB.initPublisher(self._apiKey, divId, screenSize);
                self._publisher = self._session.publish(self._publisher);
                self._session.createArchive(self._apiKey, "perSession", self._archiveName);
                
                self._publisher.addEventListener("accessAllowed", function(e){
                	OT_LayoutContainer.layout();
                });
                self._publisher.addEventListener("accessDenied", function(e){
                	OT_LayoutContainer.removeStream(divId);
                	OT_LayoutContainer.layout();
                });
            }
            self.subscribeStreams(self, e.streams);
        });
        
        self._session.addEventListener('archiveCreated', function(e) {
            self._archive = e.archives[0];
            self._session.startRecording(self._archive);
            self.setArchiveId(self);
            // self.subscribeStreams(self, e.streams);
        });

        self._session.addEventListener('streamCreated', function(e) {
        	if (self._instantiateOffset < 0){
        		self._instantiateOffset = new Date().getTime() - self._instantiateTime;
        		//alert("instantiateOffset:"+self._instantiateOffset);
        		self.setVideoOffset(self);
        	}
            self.subscribeStreams(self, e.streams);
        });
        
        self._session.addEventListener('streamDestroyed', function(event) {
			for (var i = 0; i < event.streams.length; i++) {
				// For each stream get the subscriber to that stream
				var subscribers = self._session.getSubscribersForStream(event.streams[i]);
				for (var j = 0; j < subscribers.length; j++) {
					// Then remove each stream
					var userID = parseInt(event.streams[i].connection.data);
					console.log("stream destroyed:"+userID);
					if (userID != GlobalConstants.teacherID) {
						console.log("not teacher stream, removing from student layout:"+userID);
						OT_LayoutContainer.removeStream(subscribers[j].id);
	                	OT_LayoutContainer.layout();
					}
				}
			}
        });
    },

    subscribeStreams: function(self, streams) {
        for (var i = 0; i < streams.length; i++) {
            if (streams[i].connection.connectionId == self._session.connection.connectionId) {
                return;
            }
            
            var userID = parseInt(streams[i].connection.data);

            
            var divId = 'stream' + streams[i].streamId;
            
            var screenSize = {};
            var addedStudent = false;
            if (userID == GlobalConstants.teacherID) {
            	var div = document.createElement('div');
                div.id = divId;
                self._teacherContainer.appendChild(div);
                screenSize = {width: self.SCREEN_WIDTH, height: self.SCREEN_HEIGHT }
            } else {
            	console.log("adding divId:"+divId);
                OT_LayoutContainer.addStream(divId, false);
                addedStudent = true;
            }
            self._session.subscribe(streams[i], divId, screenSize);
            
            if (addedStudent){
            	OT_LayoutContainer.layout();
            }
            
        }
    },
    
    setVideoOffset: function(self) {
        $j.ajax({
            url: "/setVideoOffset",
            type: "post",
            retryLimit: 3,
            data: {
                classId: GlobalConstants.roomID,
                videoOffset: self._instantiateOffset
            },
            error: function(xhr, textStatus, errorThrown) {
                this.tryCount++;
                if (this.tryCount <= this.retryLimit) {
                    $.ajax(this);
                    return;
                }
                return;
            }
        });
    },

    setArchiveId: function(self) {
        $j.ajax({
            url: "/setArchiveId",
            type: "post",
            retryLimit: 3,
            data: {
                classId: GlobalConstants.roomID,
                archiveId: self._archive.archiveId
            },
            error: function(xhr, textStatus, errorThrown) {
                this.tryCount++;
                if (this.tryCount <= this.retryLimit) {
                    $.ajax(this);
                    return;
                }
                return;
            }
        });
    },


    getLength: function(self) {
        var duration = 0;
        var lowest_offset = 9007199254740992;
        for (var i = 0; i < self._archiveData.length; i++) {
            if ((self._archiveData[i].length + self._archiveData[i].offset) > duration) {
                duration = self._archiveData[i].length + self._archiveData[i].offset;
            }
            if (self._archiveData[i].offset < lowest_offset) {
                lowest_offset = self._archiveData[i].offset;
            }
        }
        self._firstClipStart = lowest_offset;
        self._totalDuration = duration;
    },
    
    pause: function(self) {
        for (var i = 0; i < self._archiveData.length; i++) {
        	var player = self._replayPlayers[self._archiveData[i].video_id];
        	if (player.isPlaying()){
        		player.pause();
        		self._archiveData[i].status = 'seekReady';
        	} else {
        		self._archiveData[i].status = 'stopped';
        	}
        	
        }
    },
    
    resetPlayers: function(self) {
        for (var i = 0; i < self._archiveData.length; i++) {
        	var player = self._replayPlayers[self._archiveData[i].video_id];
        	if (player.isPlaying()){
        		player.pause();
        		
        	}
        	self._archiveData[i].status = 'stopped';
        }
    },
    

    pulse: function(self, timeElapsed) {
    	//provide a option to return a promise to ensure the videos are ready to play after seek
        for (var i = 0; i < self._archiveData.length; i++) {
            //console.log("video id:"+self._archiveData[i].video_id+", status is:"+self._archiveData[i].status);
            var player = self._replayPlayers[self._archiveData[i].video_id];
            var videoStart = self._archiveData[i].offset;
            var videoEnd = self._archiveData[i].offset + self._archiveData[i].length - 1000;
            if (self._archiveData[i].status == 'seekPending') {
            	console.log("in pulse seeking pending:"+player.id());
            	continue;
            }
            
            if (self._archiveData[i].status == 'seekReady') {
                self._archiveData[i].status = 'started';
                player.resume();
                self.showPlayer(self, player);
                self.updateArchiveVideoLayout(self, player);
            } else if (timeElapsed >= videoStart && timeElapsed <= videoEnd && self._archiveData[i].status != 'started') {
                self._archiveData[i].status = 'started'; 
                var videoId = player.id();
                $f(videoId).seek(0).play();
                
                self.showPlayer(self, $f(videoId));
            	self.updateArchiveVideoLayout(self, $f(videoId));
//                $j.wait(500).then(function() {
//                	self.showPlayer(self, $f(videoId));
//                	self.updateArchiveVideoLayout(self, $f(videoId));
//                });
                
            } else if ((timeElapsed < videoStart || timeElapsed > videoEnd) && self._archiveData[i].status != 'stopped') {
                if (player.isPlaying()) {
                    player.pause();
                }
                self.hidePlayer(self, player);
                self.updateArchiveVideoLayout(self, player);
                self._archiveData[i].status = 'stopped'
            }
        }
    },
    
    seek: function(self, position) {
        self._loadArchivedPromise = $j.Deferred();
        for (var i = 0; i < self._archiveData.length; i++) {
            var player = self._replayPlayers[self._archiveData[i].video_id];
            var videoStart = self._archiveData[i].offset;
            var videoEnd = self._archiveData[i].offset + self._archiveData[i].length - 1000;
            
            if (position >= videoStart && position <= videoEnd) {
                var seconds = Math.round((position - self._archiveData[i].offset) / 1000);
                self._archiveData[i].status = 'seekPending';
                self.showPlayer(self, player);
                self.updateArchiveVideoLayout(self, player);
                //what a piece of shit seek only works when paused?
                if (player.isPlaying()) {
                    self._desiredPos[player.id()] = seconds;
                    player.pause().seek(seconds);
                } else {
                    self._seekFlag[player.id()] = true;
                    player.seek(seconds);
                }
            } else {
                if (player.isPlaying()) {
                    player.pause();
                }
                self.hidePlayer(self, player);
                self.updateArchiveVideoLayout(self, player);
                self._archiveData[i].status = 'stopped';
            }
        }
        self.updateArchiveStatus(self);
        return self._loadArchivedPromise;
    },

    updateArchiveStatus: function(self) {
        var status = false;
        var count = 0;
        for (k in self._replayPlayers) {
            if (self._replayPlayers.hasOwnProperty(k)) {
                ++count;
            }
        }
        if (self._archiveData != null && self._archiveData.length == count) {
            status = true;
            for (var key in self._replayPlayers) {
                var state = self._replayPlayers[key].getState();
                if (state == 2) {
                    status = false;
                }
               //console.log(key+":state:"+state);
                //console.log(key+":status:"+JSON.stringify(self._replayPlayers[key].getStatus()));
                if (self._desiredPos[key] >= 0) {
                	console.log("still seeking async: not resolving");
                    status = false;
                }
                if (self._seekFlag[key] == true) {
                	console.log("still seeking: not resolving");
                    status = false;
                }
            }
        }

        if (status) {
        	//self._loadArchivedPromise.resolve();
        	console.log("resolving promise");
            setTimeout("global_video_holder._loadArchivedPromise.resolve()", 2000);
        } else {
            setTimeout("global_video_holder.updateArchiveStatus(global_video_holder)", 500);
        }
    },

    loadArchive: function(self) {
        self._loadArchivedPromise = $j.Deferred();
        $j.ajax({
            url: "/getClassArchive/" + GlobalConstants.roomID,
            type: "get",
            success: function(response) {
            	console.log("!!archive data before:"+JSON.stringify(response));
            	$j.each(response, function(index, value) { 
            		value.offset = value.offset+GlobalConstants.videoOffset+0;
            		console.log("new offset:"+value.offset);
            	});
            	console.log("archive data after:"+JSON.stringify(response));
                self._archiveData = response;
                self.initArchivePlayers(self);
                self.updateArchiveStatus(self);
                self.getLength(self);
            }
        });
        return self._loadArchivedPromise;
    },

    initArchivePlayers: function(self) {
    	$j('#studentContainer').addClass("studentArchiveVideo");
    	$j('#teacherContainer').addClass("teacherArchiveVideo");
    	
        for (var i = 0; i < self._archiveData.length; i++) {
            var id = self._archiveData[i].video_id;
            var div = document.createElement('span');
            div.id = id;
            if (self._archiveData[i].is_teacher) {
            	//div.className += ' teacherArchiveVideo';
            	self._teacherContainer.appendChild(div);
            	
            } else {
            	//div.className += ' studentArchiveVideo';
            	self._studentContainer.appendChild(div);
            };
            var player = flowplayer(id, {
                src: self._archiveData[i].s3_bucket + '/flowplayer-3.2.14.swf'
            }, {
                clip: {
                    provider: 'rtmp',
                    url: self._archiveData[i].streaming_url+"/"+id,
                    bufferLength: 3,
                    autoBuffering: true,
                    autoPlay: false,
                    scaling: 'fit',
                    onStart: function () {
                        this.getPlugin("play").hide();
                    },                
                },

                plugins: {
                    rtmp: {
                        url: self._archiveData[i].s3_bucket + '/flowplayer.rtmp-3.2.11.swf',
                        inBufferSeek: false
                    },
                    controls: null,
                    play: null
                }
            });

//            player.onPause(function() {
//                var seekPos = self._desiredPos[this.id()];
//                if (seekPos != undefined && seekPos >= 0) {
//                    this.seek(seekPos);
//                }
//            });
            
            player.onLoad(function() {
                self._replayPlayers[this.id()] = this;
                //this.startBuffering();
                console.log("hiding from onload:"+this.id());
                self.hidePlayer(self, this);
            });

            player.onSeek(function(clip, pos) {
                //this is a hack to work around flowplayer not seeking unless
            	//the player is paused
            	if (self._seekFlag[this.id()] || self._desiredPos[this.id()] >=0 ){
            		self._desiredPos[this.id()] = -1;
                    self._seekFlag[this.id()] = false;
                  // console.log("SEEK DESIRED POSITION:"+self._desiredPos[this.id()]+", SEEK FLAG: "+ self._seekFlag[this.id()]);
                    for (var i = 0; i < self._archiveData.length; i++) {
                        var key = self._archiveData[i].video_id;
                        if (key != this.id()) {
                            continue;
                        } else if (self._archiveData[i].status == "seekPending"){
                            self._archiveData[i].status = 'seekReady';
                        }
                    }
            	} 
            	this.startBuffering();
            });
            
//            player.onResume(function(clip) {
//            	console.log("PLAYER RESUME:"+this.id()+", state:"+JSON.stringify(this.getStatus()));
//            });
            
            player.onLastSecond(function(clip) {
            	//this is a hack to prevent the clip from stopping.
            	//for some reason once the clip is stopped you can no longer seek
            	// and will not respond to calls to start it.
            		console.log("Last second");
            });

            player.onBeforeFinish(function(clip) {
            	//this is a hack to prevent the clip from stopping.
            	//for some reason once the clip is stopped you can no longer seek
            	// and will not respond to calls to start it.
            		console.log("BEFORE FINISH CALLED");
            		//this.pause();
            		self._lastSecond = false;
                	console.log("CLIP ABOUT TO END:"+this.id()+", state:"+JSON.stringify(this.getStatus()));
                	
                	return false;
            });
        }

        for (var i = 0; i < self._archiveData.length; i++) {
            var player = $f(self._archiveData[i].video_id);
            player.load();
            
        }
    },


    showPlayer: function(self, player) {
        //hack flowplayer's hide/show doesnt work properly
//        var id = player.id();
//        $j('#' + id).children("object:first").height(120);
//        $f(id).getScreen().css({
//            display: 'block'
//        });
//    	var id = player.id();
//    	$j('#' + id).css({
//            height: '100%'
//        });
    	player.show();
    	var id = player.id();
    	$j('#' + id).css({
            height: ''
        });
    	var id = player.id();
    	$j('#'+id).removeClass('archiveHidden');
    },
    
    
    
    hidePlayer: function(self, player) {
        //hack flowplayer's hide/show doesnt work properly
//        var id = player.id();
//        $j('#' + id).children("object:first").height(0);
//        $f(id).getScreen().css({
//            display: 'none'
//        });
    	player.hide();
    	var id = player.id();
    	$j('#'+id).addClass('archiveHidden');
    },
    
    updateArchiveVideoLayout: function(self, player) {
    	//console.log("UPDATING VIDEO LAYOUT");
    	var id = player.id();
    	var playerElement = $j('#'+id);
    	var studentContainer = $j('#studentContainer');
    	var targetWidth = self.ARCHIVE_SCREEN_WIDTH;
    	var targetHeight = self.ARCHIVE_SCREEN_HEIGHT;
    	self._studentArchiveVisible = studentContainer.children().not(".archiveHidden").length;

    	//console.log("visible count:"+self._studentArchiveVisible);
    	if (self._studentArchiveVisible  < 2 ) {
    		self._archivesResized = false;
    		self._studentContainer.style.width = (self.ARCHIVE_SCREEN_WIDTH)+"px";
            self._studentContainer.style.height =  (self.ARCHIVE_SCREEN_HEIGHT)+"px";
    		targetWidth = self.ARCHIVE_SCREEN_WIDTH;
    		targetHeight = self.ARCHIVE_SCREEN_HEIGHT;
    	}else if (self._studentArchiveVisible > 1 ){
            self._studentContainer.style.width = (self.ARCHIVE_SCREEN_WIDTH + 50)+"px";
            self._studentContainer.style.height =  (self.ARCHIVE_SCREEN_HEIGHT + 50)+"px";
            targetWidth = Math.floor(self.ARCHIVE_SCREEN_WIDTH / 2);
    		targetHeight = Math.floor(self.ARCHIVE_SCREEN_HEIGHT / 2);
    	} 
    	
    	if (self._studentArchiveVisible > 0){
    		$j.each($j('.studentArchiveVideo span').not(".archiveHidden"), function(key, value) { 
  			  $j(value).css({width:targetWidth, height:targetHeight});
    		});
    		
    		$j.each($j('.studentArchiveVideo span').not(".archiveHidden").children("object"), function(key, value) { 
  			  $j(value).height(targetHeight);
    		});
    		
    		$j.each($j('.studentArchiveVideo .archiveHidden'), function(key, value) { 
    			  $j(value).height(0);
      		});
    	} 
    },
});
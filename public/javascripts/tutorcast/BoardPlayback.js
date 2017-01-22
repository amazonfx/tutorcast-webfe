var global_playback = {};
var playback_promise = null;
var BoardPlayback = Class.create({
	initialize : function(board, videoManager) {
		this._board = board;
		this._playbackQueue = [];
		this._alreadyPlayed = [];
		this._totalMessages = 0;
		this._messagesAppended = 0;
		this._elapsedTime = 0;
		this._playbackIntervalID = null;
		this._playbackPosition = 0;
		this._pdfFetchList = {};
		
		this._boardReady = false;
		this._videoReady = false;
		this._videoManager = videoManager;
		
		this._totalDuration = 0;
		this._boardDuration = 0;
		
		this._suppressSliderChange = false;
		this._userSliding = false;
		this._playbackPaused = false;
		
		this._seekPromise = null;
		
		this.PLAYBACK_INTERVAL = 50;
		
		this._pausePromise = {};
		this._resumePromise = {};
		
		global_playback = this;
		

		window.addEventListener('blur', function() {
			$j('#pauseReplay').click();
		},false);
		
	},
	appendPlayback : function(messages) {
		var command = {};
		var messageList = messages.split(",");
		this._totalMessages = this._totalMessages + messageList.length;
		for ( var i = 0; i < messageList.length; i++) {
			if (messageList[i].length < 1){
				++this._messagesAppended;
				continue;
			}
			var message = messageList[i]+"|true";
			//console.log("APPENDING PLAYBACK MSG:"+message);
			this._playbackQueue.push(message);
			++this._messagesAppended;
		}
	},
	
	preloadPDFList : function(self, urlList) {
		for (var i = 0; i < urlList.length; i++) {
			var url = urlList[i];
            self._pdfFetchList[url] = 0;
            self.fetchPDFUrl(self, url);
        }
	},
	
	loadVideo : function(self) {
		var loadVideoPromise = $j.when(self._videoManager.loadArchive(self._videoManager));
		loadVideoPromise.done(function() {
			self._videoReady = true;
		});
	},
	
	
	fetchPDFUrl : function(self, url) {
		var pdfManager = self._board._pdfManager;
		var loadDocPromise = $j.when(pdfManager.getPDFDocument(pdfManager, url));
		loadDocPromise.done(function(doc, purl) {
        	//console.log("promise done:"+purl);
        	delete self._pdfFetchList[purl];
        });
        loadDocPromise.fail(function(purl) {
        	var failCount = self._pdfFetchList[purl];
        	self._pdfFetchList[purl] = failCount + 1;
        	self.fetchPDFUrl(self, purl);
        });
	},
	
	finalizePlayback : function(self) {
		if (self == null || self == undefined) {
			self = this;
		}
		
		if (playback_promise == undefined || playback_promise == null){
			playback_promise = $j.Deferred();
		}
		var pdfPending = 0;
		for (key in self._pdfFetchList) {
			++pdfPending;
			if (self._pdfFetchList[key] > 3){
				playback_promise.reject("Could not prefetch PDF resource:"+key+", please refresh your page");
			} 
		}
		if (pdfPending < 1 && self._messagesAppended >= self._totalMessages && self._videoReady ) {
			if (self._playbackQueue.length > 0) {
				var queueEnd = self._playbackQueue[self._playbackQueue.length - 1];
				self._boardDuration = parseInt(queueEnd.split(",")[0]);
			} else {
				self._boardDuration = 0;
			}
			self._totalDuration = Math.max(self._boardDuration, self._videoManager._totalDuration);
			playback_promise.resolve();
		} else {
			//console.log("play back not ready sleeping:"+JSON.stringify(self._pdfFetchList));
			setTimeout("global_playback.finalizePlayback(global_playback)",
					300);
		}
		return playback_promise ;
	},
	
	updateSeekStatus : function(self) {
		if (self._videoReady && self._boardReady) {
			self._seekPromise.resolve();
			self._seekPromise = null;
			return;
		} else {
			setTimeout("global_playback.updateSeekStatus(global_playback)",
					300);
		}
	},
	
	setPlaybackPosition : function(self, percentage) {
		self._seekPromise = $j.Deferred();
		self.stopPlayback(self);
		sessionStorage.clear();
		self._videoReady = false;
		self._boardReady = false;
		self.setVideoPlaybackPosition(self, percentage);
		self.setBoardPlaybackPosition(self, percentage);
		self.updateSeekStatus(self);
		return self._seekPromise;
	},
	
	setVideoPlaybackPosition : function(self, percentage) {
		var desiredTime = Math.round((self._totalDuration * percentage) / 1000) * 1000;
		var seekPromise = $j.when(self._videoManager.seek(self._videoManager, desiredTime));
		seekPromise.done(function () {
			self._videoReady = true;
		})
	},
		
	setBoardPlaybackPosition : function(self, percentage) {
		var playQueue = [];
		var renderQueue = [];
		var queueEnd = self._playbackQueue[self._playbackQueue.length - 1];
		var nextCommand = self._playbackQueue[0];
		//round to nearest second
		var desiredTime = Math.round((self._totalDuration * percentage) / 1000) * 1000;
		
		var inserted = false;
		self._playbackPosition = 0;
		
		self._board.clearAllLayers(self._board);
		self._board._textEditor.clearAllState(self._board._textEditor);
		
		while (self._playbackQueue.length > self._playbackPosition){
			var command = self._playbackQueue[self._playbackPosition];
			var commandTime = parseInt(command.split(",")[0]);
			if (commandTime < desiredTime ){
				self.processCommands(self, command);
				++self._playbackPosition;
				inserted = true;
		    } else {
		    	break
		    }
		}

		self._elapsedTime = desiredTime;
		
		
		$j('.ui-chatbox-log').empty();
		$j('#pending-count').text('0');
    	$j('.pending-chats').hide();
    	
		self._board.updatePageDisplay(self._board, 1 ,1);
		if (inserted){
			self._board.processAllRemoteCommandsForClient(self._board, self._board.SYSTEM_CLIENT_ID);
			self._board.finalizeDeferred(self._board);
			//self._board.loadPathCanvasForPage(self._board, self._board._currentPage);
			//setTimeout("global_playback._board.finalizeDeferred(global_playback._board)", 2000);
		} 
		self._boardReady = true;
	},

	startPlayback : function(self) {
		//console.log("number of playback items:"+self._playbackQueue.length);
		if (self._playbackIntervalID != null) {
			return;
		}
		self._playbackIntervalID = setInterval(
				"global_playback.playBackCurrent(global_playback)",
				self.PLAYBACK_INTERVAL);
	},

	stopPlayback : function(self) {
		if (self._playbackIntervalID == null) {
			return;
		}
		clearInterval(self._playbackIntervalID);
		self._playbackIntervalID = null;
		//console.log("playback stopped");
	},
	
	pausePlayback : function(self) {
		if (self._playbackPaused) {
			return;
		}
		self._pausePromise = $j.Deferred();
		self._playbackPaused = true;
		self._videoManager.pause(self._videoManager);
		self.stopPlayback(self);
		setTimeout("global_playback._pausePromise.resolve()", 100);
		toastr.info("Class playback paused");
		return self._pausePromise;
	},
	
	resumePlayback : function(self) {
		if (!self._playbackPaused) {
			return;
		}
		self._resumePromise = $j.Deferred();
		self._playbackPaused = false;
		if (self._seekPromise == null) {
			self.startPlayback(self);
		}
		setTimeout("global_playback._resumePromise.resolve()", 100);
		return self._resumePromise;
	},
	
	seekComplete : function(self) {
		if (!self._playbackPaused) {
			self.startPlayback(self);
		}
	},
	
	processCommands : function(self, command) {
		self._board.addRemoteCommand(self._board, self._board.SYSTEM_CLIENT_ID, command);
	},
	
	updatePlaybackPosition : function(self, interval) {
		self._elapsedTime = self._elapsedTime + interval;
		var position = (self._elapsedTime / self._totalDuration) * 100 ;
		if (!self._userSliding) {
			self._suppressSliderChange = true;
			$j("#rewindSlider" ).slider( "option", "value", position );
			self._suppressSliderChange = false;
		} 
	},
	
	playBackCurrent : function(self) {
		if (self._elapsedTime >= self._totalDuration){
			console.log("total duration reached stopping");
			$j('#pauseReplay').click();
			//self.pausePlayback(self);
			//self._videoManager.resetPlayers(self._videoManager);
			return;
		}
		
		self._videoManager.pulse(self._videoManager, self._elapsedTime);
		self.pulseBoard(self);
		self.updatePlaybackPosition(self, self.PLAYBACK_INTERVAL);
		
	},	
	
	pulseBoard : function(self){
		if (self._playbackQueue.length > 0){
			var continueShifting = true;
			var commandInserted = false;
			while (self._playbackQueue.length > self._playbackPosition && continueShifting) {			
				var currentCommand = self._playbackQueue[self._playbackPosition];
				
				var currentTime = parseInt(currentCommand.split(",")[0]);
				if (currentTime <= self._elapsedTime) {
					//console.log("SHIFTING ");
					commandInserted = true;
					self.processCommands(self, currentCommand);
					++self._playbackPosition
				} else {
					//console.log("NOT SHIFTING CURRENT TIME:"+currentTime+", elapsed time:"+self._elapsedTime+", CURRENT COMMAND:"+currentCommand);
					continueShifting = false;
				}	
			}
			if (commandInserted){
				self._board.processAllRemoteCommandsForClient(self._board, self._board.SYSTEM_CLIENT_ID);
				self._board.finalizeDeferred(self._board);
			}
		}
	},	

});

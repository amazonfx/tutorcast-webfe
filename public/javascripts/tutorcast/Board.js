var global_holder = {};
var Board = Class.create({
    initialize: function(boardId, controlId, videoContainerId, width, height) {
        this.BROADCAST_FINE_INTERVAL = 100;
        //this._simulateLatency = true;
        this._simulateLatency = false;
        
        this.BROADCAST_COARSE_INTERVAL = 250;
        //this.PROCESS_REMOTE_INTERVAL = 10;
        this.SYSTEM_CLIENT_ID = 99999;
        this._userData = {};

        this._container = document.getElementById(boardId);
        this._pdfDocumentList = [];

        $j('#' + boardId).append($j('#boardTemplateBlock').children(":first").clone());
        $j('#boardTemplateBlock').empty();

        $j('#' + controlId).append($j('#controlTemplateBlock').children(":first").clone());
        $j('#controlTemplateBlock').empty();

        this._textDiv = document.getElementById("editor");
        $j('#editor').height(height);
        $j('#editor').width(width);

        this._backgroundCanvas = document.getElementById("background_canvas");
        this._backgroundCanvas.height = height;
        this._backgroundCanvas.width = width;
        this._kickUnpaid = false;
        this._warnUnpaid10 = false;
        this._warnUnpaid5 = false;

        this._pathCanvas = document.getElementById("path_canvas");
        this._pathCanvas.height = height;
        this._pathCanvas.width = width;

        this._backgroundContext = this._backgroundCanvas.getContext("2d");
        this._pathContext = this._pathCanvas.getContext("2d");

        this._backgroundCanvasTemp = document.createElement("canvas");
        this._pathCanvasTemp = document.createElement("canvas");
        
        this._tempPathCanvasMap = {};
        this._backgroundCanvasTemp.width = width;
        this._pathCanvasTemp.width = width;

        this._backgroundCanvasTemp.height = height;
        this._pathCanvasTemp.height = height;

        this._backgroundContextTemp = this._backgroundCanvasTemp.getContext("2d");
        this._pathContextTemp = this._pathCanvasTemp.getContext("2d");

        // If IE8, do IE-specific canvas initialization (required by
        // excanvas.js)
        /*
         * if (typeof G_vmlCanvasManager != "undefined") { this.canvas =
         * G_vmlCanvasManager.initElement(this.canvas); }
         */

        this._pathDeferred = false;
        this._backgroundDeferred = false;

        this._lineCap = "round";
        this._penDown = false;

        this._commandBuffer = [];
        this._PDFBuffer = [];

        this._orbiter = {};
        this._msgManager = {};
        this._processRemoteCommandsIntervalID = {};
        this._broadcastCommandIntervalID = {};
        this._broadcastTextID = {};
        this._broadcastPDFID = {};

        this._remoteClientCommands = {};

        this.MESSAGE_TYPE = {
            PRELOAD_PDF: "PRELOAD_PDF",
            COMMAND_UPDATE: "COMMAND_UPDATE",
            END: "END",
            REPLAY: "REPLAY",
            REPLAY_FINALIZE: "REPLAY_FINALIZE",
            SYNC_CLIENT_NEW: "SYNC_CLIENT_NEW",
            SYNC_CLIENT_UPDATE: "SYNC_CLIENT_UPDATE",
            INIT_TEXT: "INIT_TEXT",
            UPDATE_TEXT: "UPDATE_TEXT",
            UPDATE_TEXT_ACK: "UPDATE_TEXT_ACK",
            SYNC_PAGE: "SYNC_PAGE",
            INIT_PDF: "INIT_PDF",
            PDF_UPDATE: "PDF_UPDATE",
            CHAT_UPDATE: "CHAT_UPDATE",
            SYNC_USER_DATA: "SYNC_USER_DATA",
            KICK_UNPAID: "KICK_UNPAID",
            WARN_UNPAID: "WARN_UNPAID"
        };
        
        this._loadingPath = {};
        
        this._currentPage = 1;
        this._totalPages = 1;

        this.UPC = net.user1.orbiter.UPC;
        this._pen = new Pen(this);
        this._textEditor = new TextEditor(this, this._textDiv);
        this.editorToTop(this);

        this.initializeMessaging();
        this.registerListeners();
        this.initializeControls();
        this.hideIphoneTop();

        this._videoManager = new VideoManager(videoContainerId, this);
        if (GlobalConstants.replayMode) {
            this.coverBoard(this, height, width);
            this._playback = new BoardPlayback(this, this._videoManager);
        }
        this._pdfManager = new PDFManager(this);

        this._lockPage = false;
        this.updatePageDisplay(this, this._currentPage, this._totalPages);

        this._pauseEnabled = true;
        this._resumeEnabled = true;

        sessionStorage.clear();
        this.disableDrawing(this);
    },

    mapPDFToPage: function(self, url, page, boardPage) {
        self._pdfManager.initPDF(self._pdfManager, url, page, boardPage);
    },

    renderPDF: function(self, boardPage, broadcast) {
        self._pdfManager.renderPage(self._pdfManager, boardPage, broadcast);
    },

    initializeControls: function() {
        $j('#thickness').selectedIndex = 0;
        $j('#color').selectedIndex = 1;
    },

    boardStatus: function(message) {
        $j('#status').innerHtml = message;
    },
    
    getTempCanvasForPage: function(self, page) {
    	var canvasObj = self._tempPathCanvasMap[page];
    	if (!canvasObj){
    		var canvas = document.createElement("canvas");
    		canvas.height = self._pathCanvas.height;
    		canvas.width = self._pathCanvas.width;
    		var context = canvas.getContext("2d");
    		canvasObj =  {canvas:canvas, context:context};
    		self._tempPathCanvasMap[page] =canvasObj;
    		
    	}
    	return canvasObj;
    },
    
    initializeMessaging: function() {
        var self = this;
        global_holder = this;
        self._orbiter = new net.user1.orbiter.Orbiter();

        if (!self._orbiter.getSystem().isJavaScriptCompatible()) {
            self.boardStatus("Your browser is not supported.")
            return;
        }

        self._orbiter.addEventListener(
        net.user1.orbiter.OrbiterEvent.READY, function() {
            self._msgManager.addMessageListener(
            self.UPC.JOINED_ROOM, function() {
                self._broadcastCommandIntervalID = setInterval("global_holder.broadcastCommand(global_holder)", self.BROADCAST_FINE_INTERVAL);

                self._broadcastTextID = setInterval("global_holder.broadcastTextCommand(global_holder)", self.BROADCAST_COARSE_INTERVAL);
            }, self);
            self._msgManager.addMessageListener(
            self.UPC.CLIENT_REMOVED_FROM_ROOM, function(roomID, clientID) {
                delete self._remoteClientCommands[clientID];
                self._textEditor.removeClientEditor(
                self._textEditor, clientID);
            }, self);
            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.COMMAND_UPDATE, function(fromClientID, pathString) {
                var obj = JSON.parse(pathString);

                fromClientID = obj.senderId;
                var command = obj.command;
                self.processRawMessage(
                self, fromClientID, command, true);
                self.processAllRemoteCommandsForClient(
                self, fromClientID);
                self.finalizeDeferred(self);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.CHAT_UPDATE, function(fromClientID, pathString) {
                var obj = JSON.parse(pathString);
                fromClientID = obj.senderId;
                var command = obj.command;
                var holder = command.split("|");
                var decoded = decode64(holder[3]);
                var msgObj = JSON.parse(decoded);
                self.updateChat(self, msgObj);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.PDF_UPDATE, function(fromClientID, pdfString) {
                var decoded = decode64(pdfString);
                var pdfState = JSON.parse(decoded);
                self._pdfManager.updatePDFState(
                self._pdfManager, pdfState, false);
                toastr.info("A PDF document was updated.");

                $j.each(pdfState, function(key, value) {
                    var url = value.url;
                    var name = url.substring(url.lastIndexOf('/') + 1);
                    if (self._pdfDocumentList.indexOf(url) < 0) {
                        if (!self.hasFileDropdown()) {
                            self.showFileDropdown();
                        }

                        $j('#filelistDropdown').append('<li><a class="docLink" data-link="' + url + '">' + name + '</a></li>');
                        self._pdfDocumentList.push(url);
                    }
                });
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.END, function(fromClientID, resString) {
                self.handleClassEnd(self);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.SYNC_PAGE, function(fromClientID, pageString) {
                var obj = JSON.parse(pageString);

                var endOfSync = obj.isEnd;
                var desiredPage = obj.desiredPage;
                var totalPages = obj.totalPages;
                var command = obj.command;
                // alert("SYNC_PAGE:"+pathString);
                if (desiredPage != self._currentPage || self._totalPages != totalPages) {
                    self.updatePageData(
                    self, desiredPage, totalPages);
                    self.clearPermLayers(self);
                }

                if (command != undefined && command != null && command.length > 0) {
                    self.processRawMessage(
                    self, self.SYSTEM_CLIENT_ID, command, true);
                    self.processAllRemoteCommandsForClient(
                    self, self.SYSTEM_CLIENT_ID);
                }
                if (endOfSync) {
                    self.finalizeDeferred(self);
                }

            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.SYNC_CLIENT_NEW, function(fromClientID, clientString) {
                self._textEditor.updateClientEditors(
                self._textEditor, clientString, true);
            }, self, [GlobalConstants.roomID]);
        
        	self._msgManager.addMessageListener(
        	self.MESSAGE_TYPE.KICK_UNPAID, function(fromClientID, msg) {
        		 if (!self._kickUnpaid){
        			 self._kickUnpaid = true;
        			 bootbox.dialog("Your free preview of this class has ended", {
         	    	    "label" : "Ok",
         	    	    "class" : "btn-danger",   
         	    	    "callback": function() {
         	    	    	window.location = "/browseVideo";  
         	    	    }
         	    	}, {
         	    	    "animate": true
         	    	});
        		 }   
        	}, self, [GlobalConstants.roomID] );
        	
        	self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.WARN_UNPAID, function(fromClientID, msg) {
            	var obj = JSON.parse(msg);
            	if (obj.level == 10 && !self._unpaidWarn10){
            		self._unpaidWarn10 = true;
            		toastr.error("You have not yet paid for this class. Your free preview will end in 10 minutes"); 
            	} else if (obj.level == 5 && !self._unpaidWarn5){
            		self._unpaidWarn5 = true;
            		toastr.error("Your free preview will end in 5 minutes"); 
            	}
                
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.SYNC_CLIENT_UPDATE, function(fromClientID, clientString) {
                console.log("SYNC CLIENT UPDATE:" + clientString);
                self._textEditor.updateClientEditors(
                self._textEditor, clientString, false);
            }, self, [GlobalConstants.roomID]);

            
            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.SYNC_USER_DATA, function(fromClientID, userDataString) {
               //console.log("RECEIVED USER DATA UPDATE:" + userDataString);
                self._userData = JSON.parse(userDataString);
                self.updateUserData(self);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.INIT_TEXT, function(fromClientID, im) {
                var initMsg = JSON.parse(im);
                self._textEditor.initChangesetManager(
                self._textEditor, initMsg.baseRev, initMsg.baseText, initMsg.page);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.UPDATE_TEXT, function(fromClientID, um) {
                var updateMsg = JSON.parse(um);
                self._textEditor.applyChangesToEditorInstance(
                self._textEditor, updateMsg.senderID, updateMsg.revID, decode64(updateMsg.changeset), updateMsg.page);
            }, self, [GlobalConstants.roomID]);

            self._msgManager.addMessageListener(
            self.MESSAGE_TYPE.UPDATE_TEXT_ACK, function(fromClientID, am) {
                var ackMsg = JSON.parse(am);
                //alert("change acknowledged");
                self._textEditor.acknowledgeChange(
                self._textEditor, ackMsg.senderID, ackMsg.revID, decode64(ackMsg.changeset), ackMsg.page);
            }, self, [GlobalConstants.roomID]);

            // Create a room for then
            // join it
            self._msgManager.sendUPC(self.UPC.CREATE_ROOM, GlobalConstants.roomID, "_DIE_ON_EMPTY|false", null, "class|com.tutorcast.roommodules.WhiteboardModule");
            self._msgManager.sendUPC(self.UPC.JOIN_ROOM, GlobalConstants.roomID);

            var userInfo = {
                userEmail: GlobalConstants.userEmail,
                userName: GlobalConstants.firstName,
                userId: GlobalConstants.userID,
                profilePic: GlobalConstants.profilePic,
                paid:GlobalConstants.paid
            };
            //register user data
            self._msgManager.sendUPC(net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "REGISTER_USER_EVENT", "userInfo" + "|" + JSON.stringify(userInfo));

            if (GlobalConstants.replayMode) {
                self._msgManager.addMessageListener(
                self.MESSAGE_TYPE.PRELOAD_PDF, function(fromClientID, pathString) {
                    // preload pdf.
                    var pdfList = JSON.parse(pathString);
                    self._playback.preloadPDFList(
                    self._playback, pdfList);
                }, self, [GlobalConstants.roomID]);

                self._msgManager.addMessageListener(
                self.MESSAGE_TYPE.REPLAY, function(fromClientID, pathString) {
                    // append to
                    // playback buffer
                    self._playback.appendPlayback(pathString);
                }, self, [GlobalConstants.roomID]);

                self._msgManager.addMessageListener(
                self.MESSAGE_TYPE.REPLAY_FINALIZE, function(fromClientID, msg) {
                    //load video playback
                    self._playback.loadVideo(self._playback);
                    // signal end of
                    // commands
                    // start playing
                    // once all
                    // resources ready.
                    $j('#loadingModal').modal('show');
                    $j('#loadingModal').on('show', function () {
                    	  $j('img').focus()
                    	})
                    
                    var finalizePromise = $j.when(self._playback.finalizePlayback(self._playback));

                    finalizePromise.done(function() {
                        console.log("finalize promise success");
                        $j('#loadingModal').modal('hide');
                        self.renderReplayControl(self);
                        self._playback.startPlayback(self._playback);
                    });
                    finalizePromise.fail(function(
                    msg) {
                        alert(msg);
                    });
                }, self, [GlobalConstants.roomID]);

                self._msgManager.sendUPC(
                net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "REPLAY_EVENT", "arg1|val1");
            }
        }, self);
        self._orbiter.addEventListener(
        net.user1.orbiter.OrbiterEvent.CLOSE, function() {
            self._boardStatus("Disconnected from server");

            // clearInterval(self._processRemoteCommandsIntervalID);
            clearInterval(self._broadcastCommandIntervalID);
            clearInterval(self._broadcastTextID);
            // clearInterval(self._broadcastPDFID);
        }, self);
        self._msgManager = self._orbiter.getMessageManager();

        self._orbiter.connect(GlobalConstants.orbitHost, GlobalConstants.orbitPort);
    },

    registerListeners: function() {
        var self = this;
        $j(document).bind("touchstart", function(e) {
            e.preventDefault();
            self._pen.drawStart(e.originalEvent);
        });

        $j(document).bind("touchmove", function(e) {
            e.preventDefault();
            self._pen.drawMove(e.originalEvent);
        });

        $j(document).bind("touchend", function(e) {
            self._pen.drawEnd(e.originalEvent);
        });
        $j(".brushSelect").click(function(e) {
            e.preventDefault();
            self.unsetErase(self);
            var thickness = $j(this).attr('data-brush');
            var className = "tt-icon-brush-" + thickness;
            self._pen.setThickness(thickness, self._pen);
            $j("#brushSelector").removeClass();
            $j("#brushSelector").addClass("tt-icon-brush-" + thickness);
        });

        $j(".colorSelect").click(function(e) {
            e.preventDefault();
            self.unsetErase(self);
            var color = $j(this).attr('data-color');
            self._pen.setPenColor(color, self._pen);
            $j("#colorSelector").css('color', color);
        });

        $j('#prev').click(function() {
            self.prevPage(self);
        });
        $j('#next').click(function() {
            self.nextPage(self);
        });

        $j('#enableDrawing').click(function() {
            self.enableDrawing(self);
        });

        $j('#disableDrawing').click(function() {
            self.disableDrawing(self);
        });

        $j("#chatDiv").chatbox({
            title: '<i class="icon-chat icon-comment icon-white"></i> Room Chat',
            user: GlobalConstants.firstName,
            width: '97%',
            messageSent: function(id, user, msg) {
                if (!GlobalConstants.replayMode) {
                    this.boxManager.addMsg(user, msg, false);
                    self.sendChatMsg(self, user, msg);
                }
            }
        });

        $j('.eraseToggle').click(function() {
            if (!self._pen._eraseMode) {
                self.setErase(self);
            } else {
                self.unsetErase(self);
            }
        });

        $j('#filelist').hide();
        var fileAdded = false
        $j.each(GlobalConstants.materialList, function(key, value) {
            if (!self.hasFileDropdown()) {
                self.showFileDropdown();
            }
            $j('#filelistDropdown').append('<li><a class="docLink" data-link="' + value.link + '">' + value.name + '</a></li>');
            self._pdfDocumentList.push(value.link);
        });
        $j('#fileupload').fileupload({
            formData: [{
                name: 'classID',
                value: GlobalConstants.roomID
            }],
            dataType: 'json',
            done: function(e, data) {
                var response = data.result;
                console.log("response:" + JSON.stringify(response.result));
                if (response.success) {
                    self.mapPDFToPage(self, response.url, 1, self._currentPage);
                    self.renderPDF(self, self._currentPage, 1, true);
                    if (!self.hasFileDropdown()) {
                        self.showFileDropdown();
                    }
                    $j('#filelistDropdown').append('<li><a class="docLink" data-link="' + response.url + '">' + response.name + '</a></li>');
                    self._pdfDocumentList.push(response.url);
                    toastr.success("Your document was imported successfully");
                } else {
                    toastr.error("Document import failed. Make sure you uploaded a valid .PDF file");
                }

            },

            fail: function(e, data) {
                toastr.error("An error occurred while importing your file. Please try again");
            },

            add: function(e, submitData) {
                submitData.submit();
            }
        });

        $j('.docLink').live('click', function(e) {
            var url = $j(this).attr('data-link');
            downloadURLIframe(url);
        });

        $j('#saveDoc').click(function(e) {
            e.preventDefault();
            toastr.info("Board snapshot is being prepared");
            var textContent = self.getTextEditorContent(self);
            var pathContent = self.getPathImage(self);
            var pdfContent = self.getBackgroundImage(self);
            $j.ajax({
                url: "/saveTextPdf",
                type: "post",
                data: {
                    doc: textContent,
                    pathLayer: pathContent,
                    pdfLayer: pdfContent
                },
                success: function(response) {
                    toastr.success("Board snapshot was successful");
                    downloadURLIframe(response.url);
                }
            });
        });
        $j('#enableDrawing').tooltip();
        $j('#disableDrawing').tooltip();
        $j('.fileinput-button').tooltip();

        $j('#colorSelectWrapper').tooltip();
        $j('#brushSelectWrapper').tooltip();
        $j('.eraseToggle').tooltip();

        $j('#prev').tooltip();
        $j('#next').tooltip();
        $j('#saveDoc').tooltip();
    },

    sendChatMsg: function(self, user, msg) {
        var msgObj = {
            user: user,
            msg: msg
        };
        var msgStr = encode64(JSON.stringify(msgObj));
        console.log("ENCODED CHAT:" + msgStr);
        self._msgManager.sendUPC(net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "CHAT_UPDATE_EVENT", "msg" + "|" + msgStr);
    },

    updateChat: function(self, msg) {
        $j("#chatDiv").chatbox("option", "boxManager").addMsg(msg.user, msg.msg, true);
    },

    enableDrawing: function(self) {
        self.drawingToTop(self);
        $j('#disableDrawing').removeClass('active');
        $j('#enableDrawing').addClass('active');
        $j('.draw-control').show();
        self._container.onmousedown = function(e) {
            if (isTouchDevice()) {
                return;
            }

            var event = e || window.event;

            if (!self._penDown) {
                self._penDown = true;
                self._pen.drawStart(event);
            } else {
                self._pen.drawEnd(event);
                self._penDown = false;
            }

            if (event.preventDefault) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                event.cancelBubble = true
                return false;
            }
        };

        self._container.onmousemove = function(e) {
            if (isTouchDevice() || !self._penDown) {
                return;
            }
            var event = e || window.event;
            self._pen.drawMove(event);

            if (event.preventDefault) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                event.cancelBubble = true
                return false;
            }
        };
    },

    disableDrawing: function(self) {
        self.editorToTop(self);
        $j('#enableDrawing').removeClass('active');
        $j('#disableDrawing').addClass('active');
        $j('.eraseToggle').removeClass('active');
        $j('.draw-control').hide();
        self._container.onmousedown = function(e) {

        };

        self._container.onmousemove = function(e) {

        };
    },

    coverBoard: function(self, height, width) {
    	$j('#participantWrap').hide();
    	//$j('#videoContainer').css('opacity','0');
        self._backgroundCanvas.style.zIndex = "1";
        self._textDiv.style.zIndex = "3";
        self._pathCanvas.style.zIndex = "2";
        var container = document.getElementById("canvasWrapper");
        var glassDiv = document.createElement("canvas");
        glassDiv.id = "boardCover";
        container.appendChild(glassDiv);
        glassDiv.height = height;
        glassDiv.width = width;
        glassDiv.style.position = 'absolute';
        glassDiv.style.left = '0px';
        glassDiv.style.top = '0px';
        glassDiv.style.opacity = '0';
        glassDiv.style.zIndex = '100';
        $j('#canvasWrapper').keypress(function(e){
            return false;
        });
    },

    showFileDropdown: function() {
        console.log("SHOW FILE DROPDOWN CALLED");
        var list = $j('#filelist').clone();
        var menu = $j('#filelistDropdown').clone();
        $j('.topcontrol').append(list);
        $j('.topcontrol').append(menu);
        $j('#filelistTemplate').empty();
        $j('#filelist').show();
        $j('#filelist').tooltip();
    },

    hasFileDropdown: function() {
        if ($j('.topcontrol').has('#filelist').length > 0) {
            return true;
        } else {
            return false;
        }
    },

    drawingToTop: function(self) {
        self._backgroundCanvas.style.zIndex = "1";
        self._textDiv.style.zIndex = "2";
        self._pathCanvas.style.zIndex = "3";
    },

    editorToTop: function(self) {
        self._backgroundCanvas.style.zIndex = "1";
        self._textDiv.style.zIndex = "3";
        self._pathCanvas.style.zIndex = "2";
    },

    broadcastCommand: function(self) {
        var bufferedPath = self._commandBuffer;
        if (bufferedPath.length == 0) {
            return;
        }
        self._commandBuffer = [];
        self._msgManager.sendUPC(
        net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "COMMAND_UPDATE_EVENT", "command" + "|" + bufferedPath.join(","));
    },

    broadcastPDF: function(self) {
        var buffered = self._PDFBuffer;
        if (buffered.length == 0) {
            return;
        }

        self._PDFBuffer = [];
        self._msgManager.sendUPC(
        net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "PDF_UPDATE_EVENT", "pdf" + "|" + buffered.join(","));
    },

    broadcastTextCommand: function(self) {
        var cm = self._textEditor.getChangesetManager(self._textEditor, self._currentPage);
        //cm.submitChange(cm);
        var cs = cm.getChangesToSubmit(cm);
        if (cs != null) {
            console.log("sending changeset:" + JSON.stringify(cs));
            if (self._simulateLatency) {
                $j.wait(20000).then(function() {
                    self._msgManager.sendUPC(
                    net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "TEXT_UPDATE_EVENT", "revision" + "|" + JSON.stringify(cs));
                    cm.clearChangesToSubmit(cm);
                });
            } else {
                self._msgManager.sendUPC(
                net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "TEXT_UPDATE_EVENT", "revision" + "|" + JSON.stringify(cs));
                cm.clearChangesToSubmit(cm);
            }
        }

    },

    updatePageData: function(self, currentPage, totalPages) {
        if (totalPages != undefined && totalPages != null) {
            self._totalPages = totalPages;
        }
        self._currentPage = currentPage;
        self._textEditor.addChangesetManager(self._textEditor, currentPage);
        self._textEditor.setActivePage(self._textEditor, self._currentPage);

        self._pen.unsetErase(self._pen);

        if (currentPage > self._totalPages) {
            self._totalPages = currentPage;
        }
        self.updatePageDisplay(self, currentPage, self._totalPages);
    },

    updatePageDisplay: function(self, currentPage, totalPages) {
        self._currentPage = currentPage;
        self._totalPages = totalPages;
        $j('#statusBar').text(
        self._currentPage + '/' + self._totalPages);
        self.disableDrawing(self);
    },

    prevPage: function(self) {
        if (self._currentPage > 1) {
            self.requestPage(self, self._currentPage - 1);
        }
    },

    nextPage: function(self) {
        self.requestPage(self, self._currentPage + 1);
    },

    requestPage: function(self, desiredPage) {
        if (self._lockPage) {
            return;
        }
        self._msgManager.sendUPC(
        net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "PAGE_CHANGE_EVENT", "desiredPage" + "|" + desiredPage);
    },

    addRemoteCommand: function(self, clientID, command, deferred) {
        if (deferred == undefined || deferred == null) {
            deferred = false;
        }
        var message = command + "|" + deferred;
        if (self._remoteClientCommands[clientID] == undefined) {
            self._remoteClientCommands[clientID] = [];
        }
        self._remoteClientCommands[clientID].push(message);
    },

    processAllRemoteCommandsForClient: function(self, clientID) {
        var command;
        while (self._remoteClientCommands[clientID].length > 0) {
            command = self._remoteClientCommands[clientID].shift();
            self.processCommand(self, command);
        }
    },

    processCommand: function(self, command, replayTexts) {
        if (replayTexts == undefined || replayTexts == null) {
            replayTexts = GlobalConstants.replayMode;
        }
        //console.log("PROCESSING COMMAND:"+command);
        var holder = command.split("|");
        switch (holder[1]) {
        case "P":
            // TIME|TYPE|page|color|thickess|old_coord|new_coord|gco|deferred
            var from = holder[5].split("-");
            var dest = holder[6].split("-");
            var globalCompositeOut = holder[7]
            // var data = command.arg;
            //console.log("Draw command with gco:"+globalCompositeOut+", deferred:"+holder[8]);
            var page = parseInt(holder[2]);
            if (page == self._currentPage) {
                self.drawLine(self, holder[3], holder[4], parseInt(from[0]), parseInt(from[1]), parseInt(dest[0]), parseInt(dest[1]), parseBool(holder[8]), globalCompositeOut);
            }
            break;
        case "T":
            // TIME|TYPE|page|senderID|rev|csStart|csEnd|changeset|deferred
            if (replayTexts) {
                var rev = holder[4];
                var page = parseInt(holder[2]);
                var csStart = holder[5];
                var csEnd = holder[6];
                var changeset = decode64(command.substring(csStart, csEnd));
                self._textEditor.applyChangesToEditorInstance(
                self._textEditor, self._textEditor._myClientId, rev, changeset, page);
            }
            break;
        case "CH":
            // TIME|TYPE|COMMAND
            var decoded = decode64(holder[3]);
            var msgObj = JSON.parse(decoded);
            self.updateChat(self, msgObj);
            break;
        case "CP":
            // TIME|TYPE|currentpage|totalpage|deferred
            if (replayTexts) {

                var page = parseInt(holder[2]);
                var totalPages = parseInt(holder[3]);
                //console.log("PAGE CHANGE ATTEMPT:"+page)
               // self.savePathCanvasForPage(self, self._currentPage);
                var deferred = parseBool(holder[4]);
                self.updatePageData(self, page, totalPages);

                self.clearPermLayers(self);
                self.setPathCanvas(self, page);
                if (!deferred) {
                    self._pdfManager.renderPage(self._pdfManager, page, false);
                    
                } else {
                    self._pdfManager.setDirty(self._pdfManager, page);
                }
            }
            break;
        case "DS":
            // TIME|TYPE|page|state|deferred
            var decoded = decode64(holder[3]);
            var state = JSON.parse(decoded);
            var deferred = parseBool(holder[4]);
            self._pdfManager.updatePDFState(self._pdfManager, state, deferred);
            break;
        }
    },

    processRawMessage: function(self, fromClientID, messages, deferred) {
        if (fromClientID == null || fromClientID == undefined) {
            fromClientID = self.SYSTEM_CLIENT_ID;
        }
        if (deferred == undefined || deferred == null) {
            deferred = false;
        }
        var messageList = messages.split(",");
        for (var i = 0; i < messageList.length; i++) {
            var message = messageList[i];
            var type = message.split("|")[1];
            switch (type) {
            case "P":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            case "CH":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            case "T":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            case "CP":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            case "D":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            case "DS":
                self.addRemoteCommand(self, fromClientID, message, deferred);
                break;
            }
        }
    },

    drawLine: function(self, color, thickness, x1, y1, x2, y2, deferred, gco) {
        if (deferred == undefined || deferred == null) {
            deferred = false;
        }
        var tempCanvasObj = self.getTempCanvasForPage(self, self._currentPage);
        var context = tempCanvasObj.context;
        var tempCanvas = tempCanvasObj.canvas;
//        var context = {};
//        context = self._pathContextTemp;
        if (gco != undefined && gco != null) {
            context.globalCompositeOperation = gco;
        }
        //console.log("GCO:"+context.globalCompositeOperation);
        context.lineCap = self._lineCap;
        context.strokeStyle = color;
        context.lineWidth = thickness;
        context.beginPath();
        context.moveTo(x1, y1)
        context.lineTo(x2, y2);
        context.stroke();

        if (!deferred) {
            self._pathContext.globalCompositeOperation = "copy";
            self._pathContext.drawImage(tempCanvas, 0, 0);
            self._pathContext.globalCompositeOperation = self._pen._globalCompositeOp;
        } else {
            self._pathDeferred = true;
        }
    },

    finalizeDeferred: function(self) {
    	var tempCanvasObj = self.getTempCanvasForPage(self, self._currentPage);
        var tempCanvas = tempCanvasObj.canvas;
        
        if (self._pathDeferred) {
            self._pathContext.globalCompositeOperation = "copy";
            self._pathContext.drawImage(tempCanvas, 0, 0);
            self._pathContext.globalCompositeOperation = self._pen._globalCompositeOp;
            self._pathDeferred = false;
            //self._pathCanvasTemp.width = self._pathCanvasTemp.width;
        }

        //        if (self._backgroundDeferred) {
        //            self._backgroundContext.drawImage(
        //            self._backgroundCanvasTemp, 0, 0);
        //            self._backgroundDeferred = false;
        //            self._backgroundCanvasTemp.width = self._backgroundCanvasTemp.width;
        //        }
        self._pdfManager.finalizePDFState(self._pdfManager, true);
    },

    hideIphoneTop: function() {
        if (navigator.userAgent.indexOf("iPhone") != -1) {
            setTimeout(function() {
                window.scroll(0, 0);
            }, 100);
        }
    },

    clearAllLayers: function(self) {
        self._pathCanvas.width = self._pathCanvas.width;
        self._backgroundCanvas.width = self._backgroundCanvas.width;
       // self._pathCanvasTemp.width = self._pathCanvasTemp.width;
        self._backgroundCanvasTemp.width = self._backgroundCanvasTemp.width;
        
        $j.each(self._tempPathCanvasMap , function(index, value) { 
			value.canvas.width = value.canvas.width;
		});	
    },
    
    clearPermLayers: function(self) {
        self._pathCanvas.width = self._pathCanvas.width;
        self._backgroundCanvas.width = self._backgroundCanvas.width;
       // self._pathCanvasTemp.width = self._pathCanvasTemp.width;
        self._backgroundCanvasTemp.width = self._backgroundCanvasTemp.width;
        
    },
    
    setPathCanvas: function(self, page) {
    	var tempCanvasObj = self.getTempCanvasForPage(self, self._currentPage);
        var tempCanvas = tempCanvasObj.canvas;
        self._pathContext.globalCompositeOperation = "copy";
        self._pathContext.drawImage(tempCanvas, 0, 0);
        self._pathContext.globalCompositeOperation = self._pen._globalCompositeOp;
    },

//    savePathCanvasForPage: function(self, page) {
//    	console.log("saving path for page:"+page);
//        sessionStorage.setItem(GlobalConstants.roomId + "-" + page, self._pathCanvasTemp.toDataURL());
//    },
//
//    getPathCanvasForPage: function(self, page) {
//        return sessionStorage.getItem(GlobalConstants.roomId + "-" + page);
//    },
//
//    loadPathCanvasForPage: function(self, page) {
//        var dataUrl = self.getPathCanvasForPage(self, page);
//        var currentCanvasUrl = self._pathCanvasTemp.toDataURL()
//        var imageObj = new Image();
//        imageObj.onload = function() {
//        	console.log("img loaded:"+page);
//        	console.log(imageObj);
//        	self._pathContextTemp.globalCompositeOperation = "destination-out";
//            self._pathContextTemp.drawImage(imageObj, 0, 0);
//            self._pathContext.globalCompositeOperation = "copy";
//            self._pathContext.drawImage(imageObj, 0, 0);
//            
//            var currentPaths = new Image();
//            currentPaths.onload = function() {
//            	console.log("adding current edits:");
//            	console.log(currentPaths);
//                self._pathContext.globalCompositeOperation = "source-over";
//                self._pathContext.drawImage(self._pathCanvasTemp, 0, 0);
//            };
//            currentPaths.src = dataUrl;
//            self._pathContext.globalCompositeOperation = self._pen._globalCompositeOp;
//            self._pathContextTemp.globalCompositeOperation = self._pen._globalCompositeOp;
//        };
//        
//        //console.log("DATA URL IS::"+dataUrl);
//        imageObj.src = dataUrl;
//       
//    },

    lockPage: function(self) {
        self._lockPage = true;
    },

    unlockPage: function(self) {
        self._lockPage = false;
    },

    setErase: function(self) {
        self.drawingToTop(self);
        self._pen.setErase(self._pen);
        $j('.eraseToggle').addClass('active');
    },

    unsetErase: function(self) {
        self.drawingToTop(self);
        self._pen.unsetErase(self._pen);
        $j('.eraseToggle').removeClass('active');
    },

    getBackgroundImage: function(self) {
        var data = self._backgroundCanvas.toDataURL();
        return data.replace(/^data:image\/(png|jpg);base64,/, "");
    },

    getPathImage: function(self) {
        var data = self._pathCanvas.toDataURL();
        return data.replace(/^data:image\/(png|jpg);base64,/, "");
    },

    getTextEditorContent: function(self) {
        var cloned = $j('#editor').clone();
        cloned.find(".ace_cursor-layer").remove();
        cloned.find("textarea").remove();
        //console.log("CLONED:"+cloned.outerHTML());
        return "<html><head>" + $j('head').html() + " <style> @page {  margin: 0px; } </style> </head><body>" + cloned.outerHTML() + "</body></html>";
    },
    
    paySuccess: function(self) {
    	GlobalConstants.paid = true;
    	$j('#payButton').hide();
    	self._msgManager.sendUPC(net.user1.orbiter.UPC.SEND_ROOMMODULE_MESSAGE, GlobalConstants.roomID, "NOTIFY_USER_PAID_EVENT", "userId" + "|" + GlobalConstants.userID);
    },

    updateUserData: function(self) {
    	
    	$j('#activeBlock').empty();
    	$j.each(self._userData, function(index, value) { 
    			var dataStr = '<li class="activeUser" data-placement="bottom" data-original-title="'+value.username+'" data-email="'+value.userEmail+'"> <img src="'+value.profilePic+'" class="img-polaroid">';
    			if (!value.paid) {
    				value.username+=' has not paid and will be removed after 10 minute free preview ends';
    				dataStr = '<li class="activeUser" data-placement="bottom" data-original-title="'+value.username+'" data-email="'+value.userEmail+'"> <img src="'+value.profilePic+'" class="img-polaroid">';
    				dataStr += '<span class="profile-warning">!</span>';
    			}
    			$j('#activeBlock').append(dataStr+'</li>')
    			$j('#activeParticipant').show();
    		

    		$j.each($j('#pendingBlock li'), function(index, elem) { 
    			var pendingEmail =$j(this).text();
    			console.log("CHECKING PENDING EMAIL:"+pendingEmail);
    			if (pendingEmail == value.userEmail){
    				$j(this).remove();
    			}
    		});
    		
		});
    	if ($j('#pendingBlock li').length < 1){
    		$j('#pendingParticipant').hide();
    	}
    	$j('.activeUser').tooltip(); 
    },
    
    handleClassEnd: function(self) {
        if (!GlobalConstants.replayMode) {
            alert("class has ended redirecting you");
            window.location = "/browseVideo";
        }
    },

    renderReplayControl: function(self) {
    	//$j('#videoContainer').css('opacity','1');
        $j('#pauseReplay').click(function() {
            if (!self._pauseEnabled) {
                return;
            }
            self._pauseEnabled = false;
            var pausePromise = $j.when(self._playback.pausePlayback(self._playback));
            pausePromise.done(function() {
                $j('#pauseReplay').hide();
                $j('#resumeReplay').show();
                self._pauseEnabled = true;
            })

        });

        $j('#resumeReplay').click(function() {
            if (!self._resumeEnabled) {
                return;
            }
            self._resumeEnabled = false;
            var resumePromise = $j.when(self._playback.resumePlayback(self._playback));
            resumePromise.done(function() {
                $j('#pauseReplay').show();
                $j('#resumeReplay').hide();
                self._resumeEnabled = true;
            })
        });
        $j('#playbackControl').show();
        $j('#pauseReplay').show();
        $j('#resumeReplay').hide();
        $j('#rewindSlider').show();

        $j("#rewindSlider").slider({
            start: function(event, ui) {
                self._playback._userSliding = true;
            },
            stop: function(event, ui) {
                self._playback._userSliding = false;
            },
            change: function(event, ui) {
                if (self._playback._suppressSliderChange) {
                    return;
                }
                $j("#rewindSlider").slider('disable');
                var percentage = ui.value / 100;
                $j('#loadingModal').modal('show');
                var seekPromise = $j.when(self._playback.setPlaybackPosition(self._playback, percentage));
                seekPromise.done(function() {
                    //console.log("SEEK IS READY STARTING PLAYBACK");
                	 $j('#loadingModal').modal('hide');
                    self._playback.seekComplete(self._playback);
                    $j("#rewindSlider").slider('enable');
                });
                seekPromise.fail(function() {
                    $j("#rewindSlider").slider('enable');
                });
            }
        });
        $j('#pauseReplay').tooltip();
        $j('#resumeReplay').tooltip();
        $j('#rewindSlider').tooltip();
    },
});
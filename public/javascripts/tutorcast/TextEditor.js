var TextEditor = Class.create({
    initialize: function(board, editorDiv) {
        this._board = board;
        this._editorDiv = editorDiv
        this._editor = null;
        this._initialEditorValue = null;
        this._clientList = [];
        this._clientCursorList = {};

        this._activePage = 1;

        this._myClientId = -1;

        this._changesetManagerMap = {};
        this.addChangesetManager(this, this._activePage);

        this._suppress = false;
    },

    getChangesetManager: function(self, page) {
        var changeset = self._changesetManagerMap[page];
        if (changeset == undefined || changeset == null) {
            self.addChangesetManager(self, page);
            changeset = self._changesetManagerMap[page];
        }
        return changeset;
    },

    addChangesetManager: function(self, page) {
        if (self._changesetManagerMap[page] == undefined || self._changesetManagerMap[page] == null) {
            self._changesetManagerMap[page] = new ChangesetManager(self._board, page);
        } 
    },
    
    clearAllState: function(self) {
    	 self._changesetManagerMap = {};
    	 self._activePage = 1;
    	 self._suppress = true;
    	 self._editor.getSession().setValue("");
    	 self._suppress = false;
    },

    setActivePage: function(self, page) {
        self._activePage = page;
        var changesetManager = self.getChangesetManager(self, self._activePage);
        self.setEditorValue(self, changesetManager);
    },

    applyChangesToEditorInstance: function(self, clientId, revision, changeset, page) {
        var changesetManager = self.getChangesetManager(self, page);
       // console.log("got changesetmanager for page:"+page);
//        var editor = self._editor;
//        var cs = changesetManager.prepareRemoteChange(revision, changeset, changesetManager);
//        if (self._activePage == page && cs != null) {
//            self._suppress = true;
//            var mid = editor.applyChangeset(cs, self._clientCursorList[clientId], self._clientList.indexOf(clientId));
//            self._clientCursorList[clientId] = mid;
//            self._suppress = false;
//        } 
        
        
        var changeObj = changesetManager.prepareRemoteChange(revision, changeset, changesetManager);
        if (self._activePage == page && changeObj!=null) {
            self._suppress = true;
            var pos = self._editor.getCursorPosition();
            self._editor.getSession().setValue(changeObj.text);
            self._editor.moveCursorTo(pos.row, pos.column);
            var mid = self._editor.showChangesetCursor(changeObj.cs, self._clientCursorList[clientId], self._clientList.indexOf(clientId))
            self._clientCursorList[clientId] = mid;
            self._suppress = false;
        }
    },

    setEditorValue: function(self, changesetManager) {
        var editor = self._editor;
        var baseText = changesetManager._baseText;
        if (editor != undefined && editor != null) {
            self._suppress = true;
            editor.getSession().setValue(baseText);
            self._suppress = false;
        } else {
            self._initialEditorValue = baseText;
        }
    },

    initChangesetManager: function(self, revision, baseText, page) {
        var changesetManager = self.getChangesetManager(self, page);
        changesetManager.initState(revision, baseText, changesetManager);
        if (page == self._activePage) {
            self.setEditorValue(self, changesetManager);
        }
    },

    isInitialized: function(self, page) {
        var changesetManager = self.getChangesetManager(self, page);
        return self._changesetManager.isInitialized(changesetManager);
    },

    acknowledgeChange: function(self, clientId, revision, changeset, page) {
        var changesetManager = self.getChangesetManager(self, page);
        var changeObj = changesetManager.acknowledgeChange(revision, changeset, changesetManager);
        if (self._activePage == page && changeObj!=null) {
            self._suppress = true;
            var pos = self._editor.getCursorPosition();
            self._editor.getSession().setValue(changeObj.text);
            self._editor.moveCursorTo(pos.row, pos.column);
            var mid = self._editor.showChangesetCursor(changeObj.cs, self._clientCursorList[clientId], self._clientList.indexOf(clientId))
            self._clientCursorList[clientId] = mid;
            self._suppress = false;
        }
//        
//        var editor = self._editor;
//        var cs = changesetManager.acknowledgeChange(revision, changeset, changesetManager);
//        if (self._activePage == page && cs != null) {
//            self._suppress = true;
//            var mid = editor.applyChangeset(cs, self._clientCursorList[clientId], self._clientList.indexOf(clientId));
//            self._clientCursorList[clientId] = mid;
//            self._suppress = false;
//        } 
    },

    getEditorInstance: function(self, clientId) {
        require(["ace/ace"], function(ace) {
            var editor = ace.edit(self._editorDiv, clientId);
            editor.renderer.setShowGutter(false);
          //  editor.setHighlightActiveLine(false);
            
            editor.renderer.setHScrollBarAlwaysVisible(false);
            editor.session.setUseWrapMode(true);
            editor.setShowPrintMargin(false);
            editor.setEditorId(clientId);
            editor.setFontSize('13px');
            //alert("LINE HEIGHT:"+editor.renderer.lineHeight);
            self.registerListeners(self, editor);
            
            self._editor = editor;
            editor.renderer.lineHeight = 15;
            
            var initialText = self._initialEditorValue;
            if (initialText != undefined && initialText != null) {
                self._suppress = true;
                editor.getSession().setValue(initialText);
                self._suppress = false;
            }
            return editor;
        });
    },

    updateClientEditors: function(self, clients, isNew) {
        var clientList = clients.split("|");
        self._clientList = clientList;
        if (isNew) {
            self._myClientId = clientList[0];
            self.getEditorInstance(self, self._myClientId);
        }
        self._clientList.sort();
    },


    removeClientEditor: function(self, clientId) {
        self._clientList = removeArrayValues(self._clientList, clientId);
        self._editor.getSession().removeMarker(self._clientCursorList[clientId]);
        delete self._clientCursorList[clientId];
        self._clientList.sort();

    },

    handleChangeEvent: function(self, ev) {
        if (self._suppress) {
            return;
        }
        //apply changeset
        var clientId = ev.sid;
        var changeset = ev.data.easySync;
        var changesetManager = self.getChangesetManager(self, self._board._currentPage);
        changesetManager.updateUserChange(changeset, changesetManager);
    },

    registerListeners: function(self, editor) {
        editor.getSession().on('change', function(e) {
        	//console.log("CHANGE EVENT:"+JSON.stringify(e));
            self.handleChangeEvent(self, e);
        });
    }

});
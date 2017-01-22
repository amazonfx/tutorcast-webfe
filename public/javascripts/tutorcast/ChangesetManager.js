var ChangesetManager = Class
		.create({
			initialize : function(board, page) {
				this._revision = 0;
				this._baseText ="";
				this._board = board;
				this._page = page;
				
				this._userChanges = Changeset.identity(0);
				this._submittedChanges = Changeset.identity(0);
				this._changesToSubmit = null;
				
				this._changesAccepted = true;
				this._hasPendingChanges = false;
				
				this._initialized = false;
				
				this._hasRemoteChanges = false;
				this._remoteChanges = null;
			},
			
			initState : function(revision, baseText,  self) {					
				self._revision = revision;
				self._baseText = baseText;
				self._userChanges = Changeset.identity(baseText.length);
				self._submittedChanges = Changeset.identity(baseText.length);
				self._initialized = true;
			},
			
			isInitialized : function(self) {					
				return self._initialized;
			},
			
			updateUserChange : function(cs, self) {					
				if (Changeset.isIdentity(cs)) {
					return;
				} else {
					//console.log("current user changes:"+self._userChanges+",  user change to compose:"+cs);
					self._userChanges = Changeset.compose(self._userChanges, cs, null);
					//console.log("composed user changes:"+self._userChanges);
				}
			},
			
			submitChange : function(self) {	
				//console.log("user changes:"+JSON.stringify(self._userChanges));
				if (Changeset.isIdentity(self._userChanges)) {
					return;
				}
				if (self._changesToSubmit != null){
					self._changesToSubmit = Changeset.compose(self._changesToSubmit, self._userChanges, null);
				} else {
					self._changesToSubmit = self._userChanges;
				}
				//self._baseText = Changeset.applyToText(self._submittedChanges, self._baseText);
				self._submittedChanges = self._userChanges;
				self._userChanges = Changeset.identity(Changeset.newLen(self._changesToSubmit));
				self._hasPendingChanges = true;
			},
			
			getChangesToSubmit : function(self) {		
				if (self._changesAccepted) {
					self.submitChange(self);
					if (self._changesToSubmit == null){
						return null;
					}
					self._changesAccepted = false;
					return {page:self._board._currentPage, rev:self._revision, changeset:self._changesToSubmit};
				} else {
					return null;
				}
			},
			
			clearChangesToSubmit : function(self) {	
				self._hasPendingChanges = false;
				self._changesToSubmit = null;
			},
			
			acknowledgeChange : function(revision, B, self) {
				console.log("server acknowledged revision:"+revision+": "+B);
				var finalText = null;
				if (self._hasRemoteChanges){
					finalText = self.applyRemoteChange(self._remoteChanges, self);
				}
				
				self._baseText = Changeset.applyToText(self._submittedChanges, self._baseText);
				self._submittedChanges = Changeset.identity(self._baseText.length);
				self._revision = revision;
				
				self._changesAccepted = true;
				return finalText;
			},
			
			applyRemoteChange : function(B, self) {
				var Xprime = Changeset.follow(B, self._submittedChanges, false, null);
				var XB = Changeset.follow(self._submittedChanges, B, false, null);
				var Yprime = Changeset.follow(XB, self._userChanges, false, null);
				//var D = Changeset.follow(self._userChanges,XB,  false, null);
				self._baseText = Changeset.applyToText(B, self._baseText);
				self._submittedChanges = Xprime;
				self._userChanges = Yprime;
				
				var finalText = Changeset.applyToText(self._submittedChanges, self._baseText);
				finalText = Changeset.applyToText(self._userChanges, finalText);
				self._hasRemoteChanges = false;
				self._remoteChanges = null;
				
				return {text:finalText, cs:B};
			},
			
			prepareRemoteChange : function(revision, B, self) {
				//console.log("RECEIVED revision B:"+revision+": "+B);
				if (!self._changesAccepted){
					//console.log("Deferring remote revision");
					self._hasRemoteChanges = true;
					if (self._remoteChanges == null){
						self._remoteChanges = B;
					} else {
						//console.log("composing revision:"+self._remoteChanges+" with "+B);
						self._remoteChanges = Changeset.compose(self._remoteChanges, B, null);
					}
					return null;
				}
				var finalText = self.applyRemoteChange(B, self);
				self._revision = revision;
				return finalText;
			},
		});
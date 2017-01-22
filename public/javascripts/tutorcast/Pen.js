var Pen = Class
		.create({
			initialize : function(board) {
				this._canvas = board._pathCanvas;
				this._activePointers = 0;
				this._pointers = {};
				this._board = board;
				
				this._defaultThickness = 1;
				this._maxThickness = 12;
				this._color = "#06A4ED";
				this._globalCompositeOp = "source-over";
				this._penThickness = this._defaultThickness;
				
				this._eraseMode = false
				this._colorHolder = null;
				this._thicknessHolder = null;

				this._lastBufferTime = new Date().getTime();
				// max granularity on remote event publishing
				this.MIN_PUBLISH_INTERVAL = 0;
				
			},

			appendToSendBuffer : function(lastx, lasty, newx, newy, gco) {					
				this._lastBufferTime = new Date().getTime();					
				// TYPE|page|color|thickess|old_coord|new_coord|gco
				var msg = "P|"+this._board._currentPage+"|"+this._color+"|"+this._penThickness+"|"+lastx + "-" + lasty+"|"+newx + "-" + newy+"|"+gco;
				//console.log("adding to send buffer: "+ msg);
				this._board._commandBuffer.push(msg);					
			},

			setPenColor : function(color, self) {
				self._color = color;
			},

			setThickness : function(val, self) {
				value = parseInt(val);
				var thickness = isNaN(value) ? self._defaultThickness : value;
				self._penThickness = Math.max(1, Math.min(thickness, self._maxThickness));
			},
			
			setGlobalCompositeOp : function (val, self) {
				self._globalCompositeOp = val;
			},

			getSendBuffer : function() {
				return this._sendBuffer;
			},
			
			setErase : function(self) {
				if (self._eraseMode){
					return;
				}
				self._colorHolder = self._color;
				self._thicknessHolder = self._penThickness;
				
				self._globalCompositeOp = "destination-out";
				self._penThickness = 20;
				self._eraseMode = true;
			},
			
			unsetErase : function(self) {
				if (!self._eraseMode){
					return;
				}
				self._globalCompositeOp = "source-over";
				if (self._colorHolder != null) {
					self._color = self._colorHolder;
				} else {
					self._color = "#AAAAAA";
				}
				if (self._thicknessHolder != null){
					self._penThickness = self._thicknessHolder;
				} else {
					self._penThickness = self._defaultThickness;
				}
				self._eraseMode = false;
			},
			
			getSendBuffer : function() {
				return this._sendBuffer;
			},

			drawStart : function(e) {
				if (isTouchDevice() && ~e.type.indexOf('mouse'))
					return false;
				if (e.button && e.button !== 0) {
					e.preventDefault();
					return false;
				}
				this._activePointers++;
				for ( var x = 0; x < (e.touches ? e.touches.length : 1); x++) {
					var id = e.touches ? e.touches[x].identifier : 'mouse';
					this._pointers[id] = {
						enabled : true,
						last : {
							x : getPos(e, x, this._canvas).x,
							y : getPos(e, x, this._canvas).y
						}
					};
				}
			},

			drawMove : function(e) {
				if (isTouchDevice() && ~e.type.indexOf('mouse'))
					return false;
				if (e.button && e.button !== 0 || this._activePointers == 0)
					return false;
				
				var publish = false;
				if ((new Date().getTime() - this._lastBufferTime) > this.MIN_PUBLISH_INTERVAL) {					
					publish = true;				
				}
				var num_pointers = e.touches ? e.touches.length : 1;
				for ( var i = 0; i < num_pointers; i++) {
					var id = e.touches ? e.touches[i].identifier : 'mouse', pointer = this._pointers[id];
					var pointer = this._pointers[id];
					if (pointer.enabled) {
						var pos = getPos(e, i, this._canvas)
						var last = pointer.last;
						if (publish){
							this.appendToSendBuffer(last.x, last.y, pos.x, pos.y, this._globalCompositeOp);
						}						
						this._board.drawLine(this._board, this._color, this._penThickness, last.x,last.y, pos.x, pos.y, false, this._globalCompositeOp);
						this._pointers[id].last.x = pos.x;
						this._pointers[id].last.y = pos.y;
					}
				}
				this._parallel_pointers = false;
			},

			drawEnd : function(e) {
				if (isTouchDevice() && ~e.type.indexOf('mouse'))
					return false;
				if (e.button && e.button !== 0 || this.activePointers == 0)
					return false;
				this._activePointers--;

				var ids = [];
				if (e.touches) {
					for ( var t = 0, len = e.touches.length; t < len; t++)
						ids.push(e.touches[t].identifier);
				} else {
					ids.push('mouse');
				}
				for ( var id in this.pointers) {
					if (ids.indexOf(id) == -1)
						this._pointers[id].enabled = false;
				}
			}
		});

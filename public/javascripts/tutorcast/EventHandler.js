var EventHandler = Class.create({
	initialize : function() {
		if (EventHandler._instance) {
			throw ("Use EventHandler.get to obtain instance");
		}
		this._listeners = {};
	},
	addListener: function(type, listener){
        if (typeof this._listeners[type] == "undefined"){
            this._listeners[type] = [];
        }

        this._listeners[type].push(listener);
    },
    fire: function(event){
        if (typeof event == "string"){
            event = { type: event };
        }
        if (!event.target){
            event.target = this;
        }

        if (!event.type){
            throw new Error("Event object missing 'type' property.");
        }

        if (this._listeners[event.type] instanceof Array){
            var listeners = this._listeners[event.type];
            for (var i=0, len=listeners.length; i < len; i++){
                listeners[i].call(this, event);
            }
        }
    },
    removeListener: function(type, listener){
        if (this._listeners[type] instanceof Array){
            var listeners = this._listeners[type];
            for (var i=0, len=listeners.length; i < len; i++){
                if (listeners[i] === listener){
                    listeners.splice(i, 1);
                    break;
                }
            }
        }
    }
});

EVENT_TYPE = {
		PLAYBACK_INITIALIZED : "PLAYBACK_INIT",
		PLAYBACK_FF : "PLAYBACK_FF",
		PLAYBACK_REWIND : "PLAYBACK_REWIND",
		PLAYBACK_STOP : "PLAYBACK_STOP"
	};
EventHandler._instance = new EventHandler();
EventHandler.get = function() {
	return EventHandler._instance;
};
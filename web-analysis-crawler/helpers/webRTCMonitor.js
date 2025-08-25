(() => {
    console.log("Starting WebRTC collection")
    window.webrtcLogs = []; // Store logs in the browser context
    const STACK_LINE_REGEXP = /(\()?(http[^)]+):[0-9]+:[0-9]+(\))?/;

    function getStackTrace() {
        try {
            throw new Error();
        } catch (e) {
            const stack = e.stack.split("\n"); // Trim first lines
            stack.shift();  // remove our own intercepting functions from the stack
            stack.shift();
            stack.shift();
            return stack;
        }
    }

    function getSourceFromStack(stack) {
        const res = stack[1].match(STACK_LINE_REGEXP);
        return res ? res[2] : "UNKNOWN_SOURCE";
    }

    // Extract port number if available
    function extractPort(candidateString) {
        // 1. Check for "IP PORT" pattern (ICE candidates)
        let match = candidateString.match(/(\d+\.\d+\.\d+\.\d+)\s+(\d+)/);
        if (match) return match[2];

        // 2. Check for "a=sctp-port:<port>" (SDP messages)
        match = candidateString.match(/a=sctp-port:(\d+)/);
        if (match) return match[1];

        return "N/A"; // No port found
    }

    function saveCallData(data, type) {
        let detected = "None";

        if (data.includes("127.0.0.1")) {
            detected = "127.0.0.1";
        } else if (data.includes("::1")) {
            detected = "::1";
        } else if (data.includes("localhost")) {
            detected = "localhost";
        } else if (data.includes("0.0.0.0")) {
            detected = "0.0.0.0";
        }

        const stack = getStackTrace()

        const webRTCdata = {
            type: type,
            localhost: detected,
            port: extractPort(data),
            candidate: data,
            timestamp: new Date().toISOString(),
            source: getSourceFromStack(stack),
            stack: stack
            };

        if (window.calledWebRTC) {
            window.calledWebRTC(webRTCdata)
        } else {
            console.warn("calledWebRTC not exposed!")
        }

    }

    function saveRTCPeerConnectionData(args) {
        const stack = getStackTrace();

        const webRTCdata = {
            type: "RTCPeerConnection",
            localhost: "None",
            port: "N/A",
            candidate: JSON.stringify(args),
            timestamp: new Date().toISOString(),
            source: getSourceFromStack(stack),
            stack: stack
        };

        if (window.calledWebRTC) {
            window.calledWebRTC(webRTCdata);
        } else {
            console.warn("calledWebRTC not exposed!");
        }
    }

    // Hook RTCPeerConnection constructor safely using Proxy
    window.RTCPeerConnection = new Proxy(window.RTCPeerConnection, {
        construct(target, args) {
            saveRTCPeerConnectionData(args)
            
            return new target(...args); // Call original constructor
        }
    });

    // Hook createDataChannel to detect WebRTC channels
    const originalCreateDataChannel = RTCDataChannel.prototype.constructor;
    RTCDataChannel.prototype.constructor = function (...args) {
        const stack = getStackTrace()

        const webRTCdata = {
            type: "RTCDataChannel",
            localhost: "None",
            port: "N/A",
            candidate: JSON.stringify(args),
            timestamp: new Date().toISOString(),
            source: getSourceFromStack(stack),
            stack: stack
            };

        if (window.calledWebRTC) {
            window.calledWebRTC(webRTCdata)
        } else {
            console.warn("calledWebRTC not exposed!")
        }

        return new originalCreateDataChannel(...args);
    };

    // Hook addIceCandidate to detect localhost connections
    const originalAddIceCandidate = RTCPeerConnection.prototype.addIceCandidate;
    RTCPeerConnection.prototype.addIceCandidate = function (candidate) {
        if (candidate && candidate.candidate) {
            saveCallData(candidate.candidate, "ICECandidate");
        }
        return originalAddIceCandidate.apply(this, arguments);
    };

    // Hook setLocalDescription to detect localhost in SDP
    const originalSetLocalDescription = RTCPeerConnection.prototype.setLocalDescription;
    RTCPeerConnection.prototype.setLocalDescription = function (description) {
        if (description && description.sdp) {
            saveCallData(description.sdp, "SDP-Local");
        }
        return originalSetLocalDescription.apply(this, arguments);
    };

    // Hook setRemoteDescription to detect localhost in SDP
    const originalSetRemoteDescription = RTCPeerConnection.prototype.setRemoteDescription;
    RTCPeerConnection.prototype.setRemoteDescription = function (description) {
        if (description && description.sdp) {
            saveCallData(description.sdp, "SDP-Remote");
        }
        return originalSetRemoteDescription.apply(this, arguments);
    };
    console.log("Added WebRTC collection")
})();

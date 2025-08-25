const { timeStamp } = require('console');
const BaseCollector = require('./BaseCollector');

class WebRTCCollector extends BaseCollector {

    id() {
        return 'webRTC';
    }

    init({log}) {
        this._webRTC = []
        
        this._log = log;
    }

    async addListener(page) {
        await page.exposeFunction('calledWebRTC', webRTCdata => {
            this._webRTC.push({
                source: webRTCdata.source,
                type: webRTCdata.type,
                localhost: webRTCdata.localhost,
                port: webRTCdata.port,
                candidate: webRTCdata.candidate,
                timestamp: webRTCdata.timestamp,
                stack: webRTCdata.stack
            });
        });
    }

    /**
     * @param {{cdpClient: import('puppeteer').CDPSession, url: string, type: TargetType}} targetInfo 
     */
    addTarget({type, url}) {
    }

    getData() {
        return this._webRTC;
    }
}

module.exports = WebRTCCollector;

/**
 * @typedef TargetData
 * @property {string} url
 * @property {TargetType} type
 */

/**
 * @typedef {'page'|'background_page'|'service_worker'|'shared_worker'|'other'|'browser'|'webview'} TargetType
 */
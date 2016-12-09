// 登录接口
document.querySelector('#wx-login').onclick = function() {
    doh.login({}, function(ret) {
        alert(JSON.stringify(ret));
    });
};

// 分享接口
document.querySelector('#share-shareQZone').onclick = function() {
    var data = {
        imgUrl: 'http://www.easyicon.net/api/resize_png_new.php?id=1183728&size=128',
        title: '[TITLE]share jsbridge',
        desc: '[DESC]给QQ空间分享的内容',
        link: 'http://i.qq.com'
    };
    doh.shareQZone(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};

document.querySelector('#share-shareQQ').onclick = function() {
    var data = {
        imgUrl: 'http://www.easyicon.net/api/resize_png_new.php?id=1183728&size=128',
        title: '[TITLE]share jsbridge',
        desc: '[DESC]给QQ好友分享的内容',
        link: 'http://www.qq.com'
    };
    doh.shareQQ(data, function(ret) {
    	alert(JSON.stringify(ret));
    });
};
document.querySelector('#share-shareTimeline').onclick = function() {
    var data = {
        title: '[TITLE] From Jsbridge', // 分享标题
        desc: '[DESC] 给朋友圈分享的内容', // 分享描述
        link: 'http://ttest.m.qzone.com/get_album?aid=001QPbpe2xYPON#/album', // 分享链接
        imgUrl: 'http://d.lanrentuku.com/down/png/1504/medialoot-round-social-icons/feed.png' // 分享图标
    };
    doh.shareTimeline(data, function(ret) {
          alert(JSON.stringify(ret));
      });
};
document.querySelector('#share-shareAppMessage').onclick = function() {
    var data = {
        title: '[TITLE]share jsbridge', // 分享标题
        desc: '[DESC]给WX分享的内容', // 分享描述
        link: 'http://ttest.m.qzone.com/get_album?aid=001QPbpe2xYPON#/album', // 分享链接
        imgUrl: 'http://d.lanrentuku.com/down/png/1504/medialoot-round-social-icons/feed.png', // 分享图标
        type: '', // 分享类型,music、video或link，不填默认为link
        dataUrl: '' // 如果type是music或video，则要提供数据链接，默认为空
    };
    doh.shareAppMessage(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};

// 图片接口
var images = {
    localIds: [],
    serverIds: []
};
document.querySelector('#image-chooseImage').onclick = function() {
    doh.chooseImage({
        count: 3, // 默认9
        //sizeType: ['original', 'compressed'], // 可以指定是原图还是压缩图，默认二者都有
        sizeType: ['original', 'compressed'],
        //sourceType: ['album', 'camera'], // 可以指定来源是相册还是相机，默认二者都有
        //sourceType: ['camera']
    },
    function(ret) {
        images.localIds = ret.data.localIds; // 返回选定照片的本地ID列表，localId可以作为img标签的src属性显示图片
        //alert(images.localIds);
		alert(JSON.stringify(ret))
    });
};
document.querySelector('#image-previewImage').onclick = function() {
    doh.previewImage({
        current: 'http://img1.4443.com/allimg/160416/6-160416101053-50.jpg',
        urls: [
            'http://img3.douban.com/view/photo/photo/public/p2152117150.jpg',
            'http://img1.4443.com/allimg/160416/6-160416101053-50.jpg',
            'http://img3.douban.com/view/photo/photo/public/p2152134700.jpg'
        ]
    });
};
document.querySelector('#image-uploadImage').onclick = function() {
    if (images.localIds.length == 0) {
        alert('请先使用 chooseImage 接口选择图片');
        return;
    }
    var i = 0, length = images.localIds.length;
    images.serverIds = [];

    function upload() {
        doh.uploadImage({
            localId: images.localIds[i]
        }, function(ret){
            i++;
            if (ret.code != 0) {
                alert('upload failed, code = ' + ret.code + ', msg = ' + ret.msg);
                return;
            }
            alert('已上传：' + i + '/' + length);
            images.serverIds.push(ret.data.serverId);
            alert(images.serverIds);
            if (i < length) {
                upload();
            }
        });
    }

    upload();
};
document.querySelector('#image-downloadImage').onclick = function() {
    if (images.serverIds.length == 0) {
        alert('请先使用 uploadImage 上传图片');
        return;
    }
    var i = 0, length = images.serverIds.length;
    images.localIds = [];

    function download() {
        doh.downloadImage({
            serverId: images.serverIds[i]
        }, function(ret) {
            i++;
            alert('已下载：' + i + '/' + length);
            alert(ret.data.localId);
            images.localIds.push(ret.data.localId);
            if (i < length) {
                download();
            }
        });
    }

    download();
};

// 音频接口
var voice = {
    localId: '',
    serverId: ''
};
document.querySelector('#audio-startRecord').onclick = function() {
     doh.startRecord();
   setTimeout(function () {
           alert('开始录音');
       }, 500);
     document.querySelector('#audio-startRecord').disabled=true;
};
document.querySelector('#audio-stopRecord').onclick = function() {
        doh.stopRecord(function(ret) {
        voice.localId = ret.data.localId;
        document.querySelector('#audio-startRecord').disabled=false;
    	alert(JSON.stringify(ret));
    });
};
doh.onVoiceRecordEnd(function(ret) { // 录音时间超过1分钟自动停止的回调
    alert(JSON.stringify(ret));
    document.querySelector('#audio-startRecord').disabled=false;
    voice.localId = ret.data.localId;
});

document.querySelector('#audio-playVoice').onclick = function() {
    if (voice.localId == '') {
        alert('请先使用 startRecord 接口录制一段声音');
        return;
    }
    document.querySelector('#audio-pauseVoice').disabled=false;
    document.querySelector('#audio-stopVoice').disabled=false;
    doh.playVoice({
        localId: voice.localId
    });
};
doh.onVoicePlayEnd(function(ret) { // 播放完成时的回调
   document.querySelector('#audio-stopVoice').disabled=true;
   document.querySelector('#audio-pauseVoice').disabled=true;
   alert(JSON.stringify(ret));
});
document.querySelector('#audio-pauseVoice').onclick = function() {
    if (voice.localId == '') {
        alert('请先使用 startRecord 接口录制一段声音');
        return;
    }
    document.querySelector('#audio-pauseVoice').disabled=true;
    doh.pauseVoice({
        localId: voice.localId
    });
    setTimeout(function () {
            alert(voice.localId);
        }, 500);
};
document.querySelector('#audio-stopVoice').onclick = function() {
 if (voice.localId == '') {
        alert('请先使用 startRecord 接口录制一段声音');
        return;
    }
    document.querySelector('#audio-pauseVoice').disabled=true;
    document.querySelector('#audio-stopVoice').disabled=true;
    doh.stopVoice({
        localId: voice.localId
    });
    setTimeout(function () {
            alert(voice.localId);
        }, 500);
};
document.querySelector('#audio-uploadVoice').onclick = function() {
    if (voice.localId == '') {
        alert('请先使用 startRecord 接口录制一段声音');
        return;
    }
    alert("即将上传:" + voice.localId);
    doh.uploadVoice({
        localId: voice.localId,
    }, function(ret) {
        alert(JSON.stringify(ret));
        voice.serverId = ret.data.serverId;
    });
};
document.querySelector('#audio-downloadVoice').onclick = function() {
    if (voice.serverId == '') {
        alert('请先使用 uploadVoice 上传声音');
        return;
    }
    doh.downloadVoice({
        serverId: voice.serverId,
    }, function (ret) {
       alert(JSON.stringify(ret));
       voice.localId = ret.localId;
    })
};
// 信鸽接口
document.querySelector('#xg-registerPush').onclick = function() {
    doh.registerPush({}, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-config').onclick = function() {
    var data = {
        debug: true, //打开信鸽调试开关
    };
    var u = navigator.userAgent;
    var isiOS = u.indexOf('iPhone') > -1; //iphone终端
    if (isiOS) {
        alert('iOS不支持此接口')
    }
    doh.config(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-unregisterPush').onclick = function() {
    doh.unregisterPush({}, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-setTag').onclick = function() {
    var data = {
        tag: 'test' //标签名字
    };
    doh.setTag(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-delTag').onclick = function() {
    var data = {
        tag: 'test' //标签名字
    };
    doh.delTag(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};


document.querySelector('#xg-addLocalNotification').onclick = function() {

	var yearValue;
	var monthValue;
	var dateValue;
	var hourValue;
	var minValue;
	
	function getDateValue()
	{
		var date = new Date ();
		date.setMinutes(date.getMinutes() + 1);//一分钟之后提醒
		yearValue = date.getYear() + 1900;
		monthValue = date.getMonth() + 1;
		monthValue = monthValue < 10 ? "0" + monthValue : monthValue;
		dateValue = date.getDate();
		dateValue = dateValue < 10 ? "0" + dateValue : dateValue;
		hourValue = date.getHours();
		hourValue = hourValue < 10 ? "0" + hourValue : hourValue;
		minValue = date.getMinutes();
		minValue = minValue < 10 ? "0" + minValue : minValue;
	};
	getDateValue();
	
    var data = {
        title: 'title', // 标题
        content: 'test content', // 内容
        date:yearValue+""+monthValue+""+dateValue ,//日期
        hour:hourValue , // 时间
		min: minValue+1, // 分钟
        customContent: '{\'key\':\'value\'}', // 自定义key-value
        activity: '', // 打开的activity
        ring: 1, // 是否响铃
        vibrate: 1 // 是否振动
    };
	alert(JSON.stringify(data));
    doh.addLocalNotification(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-clearLocalNotifications').onclick = function() {
    doh.clearLocalNotifications({}, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-cancelNotification').onclick = function() {
    var data = {
        nid: -1 //通知ID， -1表示清除全部通知
    };
    doh.cancelNotification(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#xg-setListener').onclick = function() {
    var data = {
        name: 'notificationShow' //message/notificationShow/notificationClick/notificationClear：设置消息透传/通知被展示/被点击/被清除的回调
    };
    doh.setListener(data, function(ret) {
        alert(JSON.stringify(ret));
    });
};

// 设备信息接口
document.querySelector('#device-getDeviceInfo').onclick = function() {
    doh.getDeviceInfo(function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#device-getNetworkType').onclick = function() {
    doh.getNetworkType(function(ret) {
        alert(JSON.stringify(ret));
    });
};

//UI接口
document.querySelector('#ui-setTitle').onclick = function() {
    doh.setTitle({title:'DOH Title'});
};

document.querySelector('#ui-showCloseButton').onclick = function() {
    doh.showCloseButton();
};
document.querySelector('#ui-hideCloseButton').onclick = function() {
    doh.hideCloseButton();
};
var menus =
{
    "menus": [
        {
            "id": "setFont",
            "name": "设置字体"
        },
        {
            "id": "refresh",
            "name": "刷新"
        },
        {
            "id": "other1",
            "name": "菜单1菜单1菜单1菜单1菜单1菜单1菜单1菜单1菜单1菜单1菜单1",
            "args": {
                "key": "key1"
            }
        }
    ]
}
document.querySelector('#ui-setOptionsMenus').onclick = function() {
    doh.setOptionsMenus(menus);
};
document.querySelector('#ui-showOptionsMenu').onclick = function() {
    doh.showOptionsMenu();
};
document.querySelector('#ui-hideOptionsMenu').onclick = function() {
    doh.hideOptionsMenu();
};
document.querySelector('#ui-onOptionsMenuClick').onclick = function() {
    doh.onOptionsMenuClick(function(ret) {
        alert(JSON.stringify(ret));
    });
};



//APP接口
var app = {
    used: '',
    result: ''
};
//APP是否安装
document.querySelector('#app-isAppInstalled').onclick = function(){
    var u = navigator.userAgent;
    var isiOS = u.indexOf('iPhone') > -1; //iphone终端
    var appName = "com.tencent.mobileqq";
    if (isiOS) {
        appName = "mqq";
    }
    doh.isAppInstalled({"name":appName},
        function(result){
            var callbackResult = JSON.stringify(result.data.isInstalled);
            app.used = 'true'
            app.result = callbackResult;
            alert(appName + " is installed: " + callbackResult);
        }
    );

}
//启动APP
document.querySelector('#app-launchApp').onclick = function(){
    if (app.used != 'true') {
        alert('请先使用 app.isAppInstalled 判断应用是否安装');
        return;
    }
    if (app.result != 'true') {
        alert('应用未安装');
        return;
    }
    var u = navigator.userAgent;
    var isiOS = u.indexOf('iPhone') > -1; //iphone终端
    var appName = "com.tencent.mobileqq";
    if (isiOS) {
        appName = "mqq";
    }
    doh.launchApp({"name":appName});
}


// 位置信息接口
document.querySelector('#location-openLocation').onclick = function() {
    var location = {
        latitude: 23.099994,
        longitude: 113.324520,
        name: 'TIT创意园',
        address: '广州市海珠区新港中路397号',
        scale: 10,
    };
    doh.openLocation(location, function(ret) {
        alert(JSON.stringify(ret));
    });
};
document.querySelector('#location-getLocation').onclick = function() {
    doh.getLocation(function(ret) {
        alert(JSON.stringify(ret));
    });
};

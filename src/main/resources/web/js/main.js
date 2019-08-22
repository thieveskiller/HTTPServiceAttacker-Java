function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] == variable) {
            return pair[1];
        }
    }
    return (false);
}

var charts = {};
var line_data_total = [];
var line_data_success = [];
var line_data_fail = [];
var line_data_time = [];
var line_net_in = [];
var line_net_out = [];

window.onload = function () {
    charts.gauge_1 = echarts.init(document.getElementById("gauge-1"));
    charts.gauge_2 = echarts.init(document.getElementById("gauge-2"));
    charts.gauge_3 = echarts.init(document.getElementById("gauge-3"));
    charts.line_1 = echarts.init(document.getElementById("line-1"));
    charts.line_2 = echarts.init(document.getElementById("line-2"));
    draw();
}

var wsaddr = getQueryVariable("ws") ? getQueryVariable("ws") : (window.location.protocol == "https:" ? "wss" : "ws") + "://" + window.location.hostname + (!window
    .location.port ? "" : (":" + window.location.port)) + "/ws";
var ws = new WebSocket(wsaddr);
ws.onclose = function () {
    ws = new WebSocket(wsaddr);
};
ws.onerror = function () {
    ws = new WebSocket(wsaddr);
}
ws.addEventListener('message', function (event) {
    var data = JSON.parse(event.data);
    // 仪表盘 内存使用
    var option = {
        series: [{
            type: 'gauge',
            max: (data.maxHeap / 1024 / 1024).toFixed(2),
            detail: {
                formatter: '{value} MB'
            },
            data: [{
                value: (data.allocatedHeap / 1024 / 1024).toFixed(2),
                name: '已分配内存'
            }]
        }]
    };
    charts.gauge_1.setOption(option, true);

    // 仪表盘 当前并发
    option = {
        tooltip: {
            formatter: "{a} <br/>{b} : {c}/s"
        },
        series: [{
            name: '业务指标',
            type: 'gauge',
            max: data.maxTotalRPS.toFixed(2),
            detail: {
                formatter: '{value}/s'
            },
            data: [{
                value: data.totalRPS.toFixed(2),
                name: '当前并发'
            }]
        }]
    };
    charts.gauge_2.setOption(option, true);

    // 仪表盘 有效请求
    option = {
        tooltip: {
            formatter: "{a} <br/>{b} : {c}/s"
        },
        series: [{
            name: '业务指标',
            type: 'gauge',
            max: data.maxVaildRPS.toFixed(2),
            detail: {
                formatter: '{value}/s'
            },
            data: [{
                value: data.vaildRPS.toFixed(2),
                name: '有效请求'
            }]
        }]
    };
    charts.gauge_3.setOption(option, true);
});

function draw() {
    // 仪表盘 内存使用
    var option = {
        tooltip: {
            formatter: "{a} <br/>{b} : {c}/s"
        },
        series: [{
            name: '业务指标',
            type: 'gauge',
            max: 5000,
            detail: {
                formatter: '{value} MB'
            },
            data: [{
                value: 0,
                name: '内存使用'
            }]
        }]
    };
    charts.gauge_1.setOption(option, true);
    // 仪表盘 当前并发
    option = {
        tooltip: {
            formatter: "{a} <br/>{b} : {c}/s"
        },
        series: [{
            name: '业务指标',
            type: 'gauge',
            max: 5000,
            detail: {
                formatter: '{value}/s'
            },
            data: [{
                value: 0,
                name: '当前并发'
            }]
        }]
    };
    charts.gauge_2.setOption(option, true);

    // 仪表盘 成功请求数
    option = {
        tooltip: {
            formatter: "{a} <br/>{b} : {c}/s"
        },
        series: [{
            name: '业务指标',
            type: 'gauge',
            max: 5000,
            detail: {
                formatter: '{value}/s'
            },
            data: [{
                value: 0,
                name: '成功请求数'
            }]
        }]
    };
    charts.gauge_3.setOption(option, true);

    // 图表 请求数
    option = {
        title: {
            text: '请求统计'
        },
        tooltip: {
            trigger: 'axis'
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        legend: {
            data: ['total', 'success', 'fail']
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: line_data_time
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: 'total',
            type: 'line',
            stack: '总量',
            smooth: true,
            data: line_data_total,
            areaStyle: {}
        }, {
            name: 'success',
            type: 'line',
            stack: '总量',
            smooth: true,
            data: line_data_success,
            areaStyle: {}
        }, {
            name: 'fail',
            type: 'line',
            stack: '总量',
            smooth: true,
            data: line_data_fail,
            areaStyle: {}
        }]
    };

    charts.line_1.setOption(option, true);

    // 图表 流量
    option = {
        title: {
            text: '流量统计'
        },
        tooltip: {
            trigger: 'axis'
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        legend: {
            data: ['in', 'out']
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: line_data_time
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: 'in',
            type: 'line',
            stack: '总量',
            smooth: true,
            data: line_net_in,
            areaStyle: {}
        }, {
            name: 'out',
            type: 'line',
            stack: '总量',
            smooth: true,
            data: line_net_out,
            areaStyle: {}
        }]
    };

    charts.line_2.setOption(option, true);
}

ws.onmessage = function (event) {
    var json = JSON.parse(event.data);
    // Speed

}
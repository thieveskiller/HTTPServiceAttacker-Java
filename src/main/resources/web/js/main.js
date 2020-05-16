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
    charts.creation_speed = echarts.init(document.getElementById("creation_speed"));
    charts.gauge_2 = echarts.init(document.getElementById("gauge-2"));
    charts.gauge_3 = echarts.init(document.getElementById("gauge-3"));
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
};
var isReceived = false;

ws.addEventListener('message', function (event) {
    isReceived = true;
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

    // 仪表盘 请求创建数
    var option = {
        series: [{
            type: 'gauge',
            max: data.maxAllowedConnections.toFixed(0),
            detail: {
                formatter: '{value}'
            },
            data: [{
                value: data.createdConnections.toFixed(0),
                name: '请求队列长度'
            }]
        }]
    };
    charts.creation_speed.setOption(option, true);

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

    line_data_success.push(data.vaildRPS);
    line_data_fail.push(data.totalRPS - data.vaildRPS);

    if (line_data_time.length > 60) {
        line_data_success.shift();
        line_data_fail.shift();
        line_data_time.shift();
    }

    charts.line_1.render();


});

setTimeout(function () {
    if (!isReceived)
        setInterval(function () {
            ws.send("ping");
        }, 500);
}, 5000);

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
    // 仪表盘 请求创建数
    var option = {
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
                name: '请求创建数'
            }]
        }]
    };
    charts.creation_speed.setOption(option, true);
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

    charts.line_1 = Highcharts.chart('line-1', {
        chart: {
            type: 'area'
        },
        title: {
            text: '请求统计'
        },
        time: {
            useUTC: false
        },
        xAxis: {
            type: 'datetime',
            tickmarkPlacement: 'on',
            title: {
                enabled: false
            }
        },
        yAxis: {
            title: {
                text: 'Requests'
            }
        },
        tooltip: {
            split: true,
            valueSuffix: ''
        },
        plotOptions: {
            area: {
                stacking: 'normal',
                lineColor: '#666666',
                lineWidth: 1,
                marker: {
                    lineWidth: 1,
                    lineColor: '#666666'
                }
            }
        },
        series: [{
            name: 'Success',
            data: line_data_success
        }, {
            name: 'Failed',
            data: line_data_fail
        }]
    });
}
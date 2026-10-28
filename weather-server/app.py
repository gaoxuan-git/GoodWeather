"""
好天气实训 — 简易服务端（3 个 GET 接口）
运行: python app.py
默认: http://0.0.0.0:5000
"""

from flask import Flask, jsonify

app = Flask(__name__)

# 课设常用城市（客户端可按 id/name 使用，天气仍由 App 直连和风）
DEFAULT_CITIES = [
    {"id": "101010100", "name": "北京", "adm1": "北京市", "adm2": "北京"},
    {"id": "101020100", "name": "上海", "adm1": "上海市", "adm2": "上海"},
    {"id": "101280101", "name": "广州", "adm1": "广东省", "adm2": "广州"},
    {"id": "101280601", "name": "深圳", "adm1": "广东省", "adm2": "深圳"},
    {"id": "101210101", "name": "杭州", "adm1": "浙江省", "adm2": "杭州"},
    {"id": "101190101", "name": "南京", "adm1": "江苏省", "adm2": "南京"},
    {"id": "101270101", "name": "成都", "adm1": "四川省", "adm2": "成都"},
    {"id": "101040100", "name": "重庆", "adm1": "重庆市", "adm2": "重庆"},
    {"id": "101110101", "name": "西安", "adm1": "陕西省", "adm2": "西安"},
    {"id": "101180101", "name": "武汉", "adm1": "湖北省", "adm2": "武汉"},
]


@app.after_request
def add_cors(resp):
    resp.headers["Access-Control-Allow-Origin"] = "*"
    return resp


@app.get("/api/health")
def health():
    return jsonify({"status": "ok", "service": "goodweather-server"})


@app.get("/api/info")
def info():
    return jsonify(
        {
            "project": "GoodWeather 移动互联网综合实训",
            "serverVersion": "1.0.0",
            "clientPackage": "com.llw.goodweather",
            "description": "简易后端：健康检查、项目信息、推荐城市列表",
        }
    )


@app.get("/api/cities")
def cities():
    return jsonify({"code": "200", "count": len(DEFAULT_CITIES), "cities": DEFAULT_CITIES})


if __name__ == "__main__":
    # Windows 上 debug+自动重载常导致 5000 在监听但 /api/health 一直超时
    app.run(host="0.0.0.0", port=5000, debug=False, use_reloader=False, threaded=True)

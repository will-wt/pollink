const API_BASE = '/api/v1/admin';

// 页面路由
const pages = {
    dashboard: renderDashboard,
    nodes: renderNodes,
    messages: renderMessages,
    configs: renderConfigs
};

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    loadPage('dashboard');

    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = e.target.dataset.page;
            loadPage(page);
            document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
            e.target.classList.add('active');
        });
    });
});

function loadPage(page) {
    const content = document.getElementById('content');
    content.innerHTML = '<p>加载中...</p>';
    if (pages[page]) {
        pages[page](content);
    }
}

async function fetchJSON(url) {
    const res = await fetch(url);
    return res.json();
}

// 概览页
async function renderDashboard(container) {
    const stats = await fetchJSON(`${API_BASE}/dashboard/stats`);
    container.innerHTML = `
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px;">
            <div class="card">
                <h3>活跃节点</h3>
                <div class="value">${stats.totalNodes}</div>
            </div>
            <div class="card">
                <h3>总连接数</h3>
                <div class="value">${stats.totalConnections}</div>
            </div>
        </div>
    `;
}

// 节点页
async function renderNodes(container) {
    const nodes = await fetchJSON(`${API_BASE}/nodes`);
    let html = '<h2>节点列表</h2><table><tr><th>ID</th><th>IP</th><th>状态</th><th>连接数</th><th>最后心跳</th></tr>';
    nodes.forEach(node => {
        html += `<tr>
            <td>${node.id}</td>
            <td>${node.ip}</td>
            <td>${node.status === 1 ? '在线' : '离线'}</td>
            <td>${node.connectionCount}</td>
            <td>${node.lastHeartbeat}</td>
        </tr>`;
    });
    html += '</table>';
    container.innerHTML = html;
}

// 消息页
async function renderMessages(container) {
    const messages = await fetchJSON(`${API_BASE}/messages?limit=20`);
    let html = `
        <h2>消息列表</h2>
        <form id="sendForm" style="margin-bottom: 20px;">
            <div class="form-group">
                <label>Topic</label>
                <input type="text" id="msgTopic" placeholder="order_notify" required>
            </div>
            <div class="form-group">
                <label>Payload</label>
                <textarea id="msgPayload" rows="3" placeholder="消息内容" required></textarea>
            </div>
            <button type="submit" class="btn btn-primary">发送测试消息</button>
        </form>
        <table><tr><th>ID</th><th>Topic</th><th>Payload</th><th>状态</th><th>时间</th></tr>`;
    messages.forEach(msg => {
        html += `<tr>
            <td>${msg.id}</td>
            <td>${msg.topic}</td>
            <td>${msg.payload}</td>
            <td>${msg.status === 0 ? '待推送' : '已推送'}</td>
            <td>${msg.create_time}</td>
        </tr>`;
    });
    html += '</table>';
    container.innerHTML = html;

    document.getElementById('sendForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await fetch(`${API_BASE}/messages/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                topic: document.getElementById('msgTopic').value,
                payload: document.getElementById('msgPayload').value
            })
        });
        renderMessages(container);
    });
}

// 配置页
async function renderConfigs(container) {
    const configs = await fetchJSON(`${API_BASE}/configs`);
    let html = `
        <h2>配置列表</h2>
        <form id="configForm" style="margin-bottom: 20px;">
            <div class="form-group">
                <label>Key</label>
                <input type="text" id="cfgKey" placeholder="feature.flag" required>
            </div>
            <div class="form-group">
                <label>Value</label>
                <textarea id="cfgValue" rows="2" placeholder="配置值" required></textarea>
            </div>
            <button type="submit" class="btn btn-primary">创建配置</button>
        </form>
        <table><tr><th>ID</th><th>Key</th><th>Value</th><th>Version</th><th>状态</th><th>操作</th></tr>`;
    configs.forEach(cfg => {
        html += `<tr>
            <td>${cfg.id}</td>
            <td>${cfg.key}</td>
            <td>${cfg.value}</td>
            <td>${cfg.version}</td>
            <td>${cfg.status === 0 ? '草稿' : (cfg.status === 1 ? '已发布' : '已回滚')}</td>
            <td>${cfg.status === 0 ? `<button class="btn btn-success" onclick="publishConfig(${cfg.id})">发布</button>` : ''}</td>
        </tr>`;
    });
    html += '</table>';
    container.innerHTML = html;

    document.getElementById('configForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await fetch(`${API_BASE}/configs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                key: document.getElementById('cfgKey').value,
                value: document.getElementById('cfgValue').value
            })
        });
        renderConfigs(container);
    });
}

async function publishConfig(id) {
    await fetch(`${API_BASE}/configs/${id}/publish`, { method: 'POST' });
    renderConfigs(document.getElementById('content'));
}

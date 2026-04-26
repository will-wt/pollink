const API_BASE = '/api/v1/admin';

// 页面路由
const pages = {
    dashboard: renderDashboard,
    nodes: renderNodes,
    messages: renderMessages,
    configs: renderConfigs,
    'gray-rules': renderGrayRules
};

// 当前页面的定时刷新器
let currentInterval = null;

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
    // 清除之前的定时刷新
    if (currentInterval) {
        clearInterval(currentInterval);
        currentInterval = null;
    }

    const content = document.getElementById('content');
    content.innerHTML = '<p>加载中...</p>';
    if (pages[page]) {
        pages[page](content);
    }
}

async function fetchJSON(url) {
    const res = await fetch(url);
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
    }
    return res.json();
}

// 概览页
async function renderDashboard(container) {
    async function update() {
        try {
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
        } catch (e) {
            container.innerHTML = `<p style="color: red;">加载失败: ${e.message}</p>`;
        }
    }
    await update();
    // 每 5 秒自动刷新
    currentInterval = setInterval(update, 5000);
}

// 节点页
async function renderNodes(container) {
    const nodes = await fetchJSON(`${API_BASE}/nodes`);
    let html = '<h2>节点列表</h2><table><tr><th>ID</th><th>IP</th><th>状态</th><th>连接数</th><th>最后心跳</th></tr>';
    nodes.forEach(node => {
        const statusMap = { 0: '离线', 1: '在线', 2: '维护中' };
        html += `<tr>
            <td>${node.id}</td>
            <td>${node.ip}</td>
            <td>${statusMap[node.status] || '未知'}</td>
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
            <button type="submit" class="btn btn-primary">创建并发布</button>
        </form>
        <table><tr><th>ID</th><th>Key</th><th>Value</th><th>Version</th><th>状态</th></tr>`;
    configs.forEach(cfg => {
        html += `<tr>
            <td>${cfg.id}</td>
            <td>${cfg.key}</td>
            <td>${cfg.value}</td>
            <td>${cfg.version}</td>
            <td>${cfg.status === 0 ? '草稿' : (cfg.status === 1 ? '已发布' : '已回滚')}</td>
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

// 灰度规则页
async function renderGrayRules(container) {
    const rules = await fetchJSON(`${API_BASE}/gray-rules`);
    let html = `
        <h2>灰度规则</h2>
        <form id="grayForm" style="margin-bottom: 20px;">
            <div class="form-group">
                <label>规则名</label>
                <input type="text" id="grName" placeholder="order_notify_10pct" required>
            </div>
            <div class="form-group">
                <label>类型</label>
                <select id="grType">
                    <option value="1">消息</option>
                    <option value="2">配置</option>
                </select>
            </div>
            <div class="form-group">
                <label>目标ID</label>
                <input type="number" id="grTargetId" placeholder="1" required>
            </div>
            <div class="form-group">
                <label>过滤条件 (JSON)</label>
                <textarea id="grFilterJson" rows="2" placeholder='{"pct": 10}' required></textarea>
            </div>
            <button type="submit" class="btn btn-primary">创建规则</button>
        </form>
        <table><tr><th>ID</th><th>名称</th><th>类型</th><th>目标ID</th><th>过滤条件</th><th>状态</th><th>操作</th></tr>`;
    rules.forEach(rule => {
        const typeLabel = rule.type === 1 ? '消息' : '配置';
        const statusLabel = rule.status === 1 ? '已启用' : '未启用';
        html += `<tr>
            <td>${rule.id}</td>
            <td>${rule.name}</td>
            <td>${typeLabel}</td>
            <td>${rule.target_id}</td>
            <td><code>${rule.filter_json}</code></td>
            <td>${statusLabel}</td>
            <td>
                ${rule.status === 0
                    ? `<button class="btn btn-success" data-action="enable" data-id="${rule.id}">启用</button>`
                    : `<button class="btn btn-primary" data-action="disable" data-id="${rule.id}">禁用</button>`}
                <button class="btn btn-danger" data-action="delete" data-id="${rule.id}">删除</button>
            </td>
        </tr>`;
    });
    html += '</table>';
    container.innerHTML = html;

    document.getElementById('grayForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await fetch(`${API_BASE}/gray-rules`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: document.getElementById('grName').value,
                type: parseInt(document.getElementById('grType').value),
                targetId: parseInt(document.getElementById('grTargetId').value),
                filterJson: document.getElementById('grFilterJson').value
            })
        });
        renderGrayRules(container);
    });

    container.querySelectorAll('button[data-action]').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const id = e.target.dataset.id;
            const action = e.target.dataset.action;
            if (action === 'enable') {
                await fetch(`${API_BASE}/gray-rules/${id}/enable`, { method: 'POST' });
            } else if (action === 'disable') {
                await fetch(`${API_BASE}/gray-rules/${id}/disable`, { method: 'POST' });
            } else if (action === 'delete') {
                await fetch(`${API_BASE}/gray-rules/${id}`, { method: 'DELETE' });
            }
            renderGrayRules(container);
        });
    });
}

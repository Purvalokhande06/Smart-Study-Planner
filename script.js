window.initApp = function() {
    setupNavigation();
    loadDashboard();
    loadSubjects();
    loadTimetable();
    loadMultiDayPlanner();

    document.getElementById('openAddSubjectBtn').addEventListener('click', () => {
        document.getElementById('addSubjectModal').classList.remove('hidden');
    });

    document.getElementById('closeAddSubjectBtn').addEventListener('click', () => {
        document.getElementById('addSubjectModal').classList.add('hidden');
    });

    document.getElementById('addSubjectForm').addEventListener('submit', handleAddSubject);
    document.getElementById('regenScheduleBtn').addEventListener('click', handleRegenerateSchedule);
};

function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            navItems.forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            const targetView = item.getAttribute('data-view');
            document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));
            document.getElementById(`view-${targetView}`).classList.add('active');

            if (targetView === 'dashboard') loadDashboard();
            if (targetView === 'subjects') loadSubjects();
            if (targetView === 'timetable') loadTimetable();
            if (targetView === 'planner') loadMultiDayPlanner();
        });
    });
}

// DASHBOARD
async function loadDashboard() {
    const resSubjects = await fetch('/api/subjects');
    const subjects = await resSubjects.json();

    const resSchedule = await fetch('/api/schedule/daily');
    const sessions = await resSchedule.json();

    document.getElementById('statSubjects').textContent = subjects.length;
    document.getElementById('statDailyHours').textContent = `${window.currentUser.dailyAvailableHours} hrs`;

    let completedSessions = sessions.filter(s => s.completed).length;
    document.getElementById('statSessionsDone').textContent = `${completedSessions} / ${sessions.length}`;

    // Top Priority Calculation
    if (subjects.length > 0) {
        document.getElementById('statTopPriority').textContent = subjects[0].name;
    } else {
        document.getElementById('statTopPriority').textContent = 'None';
    }

    // Mini Schedule
    const dashToday = document.getElementById('dashTodaySchedule');
    dashToday.innerHTML = sessions.length === 0 ? '<p>No study sessions scheduled for today.</p>' :
        sessions.slice(0, 3).map(s => `
            <div style="padding: 8px 0; border-bottom: 1px solid #eee;">
                <strong>${s.startTime} - ${s.endTime}</strong>: ${s.subjectName} (${s.topicTitle})
            </div>
        `).join('');

    // Upcoming Exams
    const dashExams = document.getElementById('dashUpcomingExams');
    dashExams.innerHTML = subjects.length === 0 ? '<p>No subjects added yet.</p>' :
        subjects.map(s => `
            <div style="padding: 8px 0; border-bottom: 1px solid #eee;">
                <strong>${s.name}</strong> - Exam: ${s.examDate}
            </div>
        `).join('');
}

// SUBJECTS
async function loadSubjects() {
    const res = await fetch('/api/subjects');
    const subjects = await res.json();
    const grid = document.getElementById('subjectListGrid');

    if (subjects.length === 0) {
        grid.innerHTML = '<p>No subjects added yet. Click "+ Add New Subject" to begin.</p>';
        return;
    }

    grid.innerHTML = subjects.map(s => `
        <div class="subject-card">
            <h3>${s.name}</h3>
            <span class="badge">Exam: ${s.examDate}</span>
            <p><strong>Difficulty:</strong> ${s.difficulty}/5 | <strong>Confidence:</strong> ${s.confidence}/5</p>
            <ul class="topic-list">
                ${s.topics.map(t => `
                    <li class="topic-item">
                        <input type="checkbox" ${t.completed ? 'checked' : ''} onchange="toggleTopic('${s.id}', '${t.id}', this.checked)">
                        <span style="${t.completed ? 'text-decoration: line-through; color: #999;' : ''}">${t.title}</span>
                    </li>
                `).join('')}
            </ul>
            <button onclick="deleteSubject('${s.id}')" class="btn btn-danger btn-small" style="margin-top: 15px;">Delete</button>
        </div>
    `).join('');
}

async function handleAddSubject(e) {
    e.preventDefault();
    const name = document.getElementById('subName').value;
    const examDate = document.getElementById('subExamDate').value;
    const difficulty = parseInt(document.getElementById('subDifficulty').value);
    const confidence = parseInt(document.getElementById('subConfidence').value);
    const topicsRaw = document.getElementById('subTopics').value;

    const topics = topicsRaw.split(',').map(t => ({ title: t.trim() })).filter(t => t.title.length > 0);

    await fetch('/api/subjects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, examDate, difficulty, confidence, topics })
    });

    document.getElementById('addSubjectModal').classList.add('hidden');
    document.getElementById('addSubjectForm').reset();
    loadSubjects();
}

async function deleteSubject(subjectId) {
    if (confirm('Are you sure you want to delete this subject?')) {
        await fetch('/api/subjects/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ subjectId })
        });
        loadSubjects();
    }
}

async function toggleTopic(subjectId, topicId, completed) {
    await fetch('/api/topics/complete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ subjectId, topicId, completed })
    });
}

// TIMETABLE
async function loadTimetable() {
    const res = await fetch('/api/schedule/daily');
    const sessions = await res.json();
    const container = document.getElementById('dailyScheduleList');

    if (sessions.length === 0) {
        container.innerHTML = '<p>No sessions scheduled. Add subjects with upcoming dates first!</p>';
        return;
    }

    container.innerHTML = sessions.map(s => `
        <div class="session-card ${s.completed ? 'completed' : ''}">
            <div>
                <h4>${s.startTime} - ${s.endTime} | ${s.subjectName}</h4>
                <p><strong>Topic:</strong> ${s.topicTitle}</p>
                <p style="font-size: 11px; color: #666;">${s.reason}</p>
            </div>
            <div>
                <input type="checkbox" ${s.completed ? 'checked' : ''} onchange="toggleSession('${s.id}', this.checked)"> Done
            </div>
        </div>
    `).join('');
}

async function handleRegenerateSchedule() {
    await fetch('/api/schedule/regenerate', { method: 'POST' });
    loadTimetable();
}

async function toggleSession(sessionId, completed) {
    await fetch('/api/sessions/complete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, completed })
    });
    loadTimetable();
}

// MULTI-DAY PLANNER
async function loadMultiDayPlanner() {
    const res = await fetch('/api/schedule/multiday');
    const plan = await res.json();

    const alertBox = document.getElementById('overloadAlert');
    if (plan.isOverloaded) {
        alertBox.classList.remove('hidden');
    } else {
        alertBox.classList.add('hidden');
    }

    const grid = document.getElementById('multiDayPlanGrid');
    grid.innerHTML = plan.days.map(d => `
        <div class="day-card">
            <h4>${d.date} (${d.dayName.substring(0,3)})</h4>
            <p style="font-size: 12px; margin-bottom: 10px;">Total Planned: ${d.totalHours} hrs</p>
            ${d.tasks.length === 0 ? '<p style="font-size: 12px; color: #999;">Rest / Free Day</p>' : 
                d.tasks.map(t => `
                    <div style="background: #f1f5f9; padding: 8px; border-radius: 6px; margin-bottom: 6px; font-size: 12px;">
                        <strong>${t.subjectName}</strong> (${t.durationHours} hrs)<br>
                        Score: ${t.priorityScore}
                    </div>
                `).join('')
            }
        </div>
    `).join('');
}
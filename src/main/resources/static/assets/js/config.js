if (typeof backendApiUrl === 'undefined') {
    var backendApiUrl = '/api';
}

function checkStatus() {
    const statusIcon = document.getElementById('indexingIcon');
    if (!statusIcon) {
        console.log('⏳ Элемент indexingIcon еще не загружен, пропускаем запрос');
        return;
    }

    fetch('/status')
        .then(response => response.json())
        .then(data => {
            const statusIcon = document.getElementById('indexingIcon');
            if (!statusIcon) return;

            switch (data.status) {
                case 'Индексация в процессе...':
                    statusIcon.className = 'status-icon blue';
                    break;
                case 'Индексация успешно завершена!':
                    statusIcon.className = 'status-icon green';
                    break;
                case 'Ошибка индексации!':
                    statusIcon.className = 'status-icon red';
                    break;
                default:
                    statusIcon.className = '';
            }
        })
        .catch(error => {
            console.warn('⚠️ Статус индексации временно недоступен:', error.message);
        });
}

function toggleIndexing(event) {
    if (event) event.preventDefault();

    const idInput = document.querySelector('input[name="id"]');
    if (!idInput) {
        console.warn('⚠️ Поле ID не найдено на странице');
        return;
    }

    const id = idInput.value.trim();

    if (!id) {
        console.warn('⚠️ ID не указан');
        return;
    }

    const button = document.getElementById('indexingButton');
    if (!button) return;

    let action = button.innerText === 'Start indexing' ? '/api/startIndexing' : '/api/stopIndexing';

    fetch(action, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ id: id }),
    })
    .then(response => {
        if (response.ok) {
            button.innerText = button.innerText === 'Start indexing' ? 'Stop indexing' : 'Start indexing';
            checkStatus();
        } else {
            console.warn('⚠️ Ошибка при изменении статуса индексации');
        }
    })
    .catch(error => {
        console.warn('⚠️ Ошибка:', error.message);
    });
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('⚙️ Config.js инициализирован');
});


if (typeof window.checkStatus === 'undefined') {
    window.checkStatus = checkStatus;
}
if (typeof window.toggleIndexing === 'undefined') {
    window.toggleIndexing = toggleIndexing;
}
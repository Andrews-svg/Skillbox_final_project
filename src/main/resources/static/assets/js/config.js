const backendApiUrl = 'api';

function checkStatus() {
    fetch('/status')
        .then(response => response.json())
        .then(data => {
            const statusIcon = document.getElementById('indexingIcon');
            
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
            console.error('Ошибка при получении статуса:', error);
            alert('Ошибка при получении статуса индексации.');
        });
}

function toggleIndexing(event) {
    event.preventDefault();
    
    const idInput = document.querySelector('input[name="id"]');
    const id = idInput.value.trim(); 
    
    if (!id) {
        alert('ID не указан. Пожалуйста, проверьте ваш запрос.');
        return;
    }

    const button = document.getElementById('indexingButton');
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
            alert('Ошибка при изменении статуса индексации.');
        }
    })
    .catch(error => {
        console.error('Ошибка:', error);
        alert('Ошибка при изменении статуса индексации.');
    });
}
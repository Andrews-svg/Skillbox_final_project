<script>
document.addEventListener('DOMContentLoaded', function () {
    const passwordInput = document.querySelector('#reg_password');
    const confirmationInput = document.querySelector('#reg_confirmPassword');
    const passwordErrorSpan = document.querySelector('#passwordError');
    const confirmationErrorSpan = document.querySelector('#confirmationError');

    // Проверка соответствия пароля нашим критериям
    function validatePassword(password) {
        let errors = [];

        if (password.length < 8)
            errors.push("Длина пароля должна быть минимум 8 символов");

        if (!/\d/.test(password))
            errors.push("Пароль должен содержать хотя бы одну цифру");

        if (!/[a-z]/.test(password) || !/[A-Z]/.test(password))
            errors.push("Пароль должен содержать буквы верхнего и нижнего регистра");

        if (!/[!@#%^&*()_+=$$$${}\\|;:'",.<>\/?]/.test(password))
            errors.push("Пароль должен содержать специальный символ");

        if (/ /.test(password))
            errors.push("Пароль не должен содержать пробелы");

        if (/123456|abcdef|qwerty/i.test(password))
            errors.push("Запрещены последовательности '123456', 'abcdef', 'qwerty'");

        return errors;
    };

    // Обработчик события onchange для первого поля пароля
    passwordInput.addEventListener('input', function () {
        const currentPassword = passwordInput.value.trim();
        const validationErrors = validatePassword(currentPassword);

        if (validationErrors.length > 0) {
            passwordErrorSpan.innerHTML = validationErrors.join('<br>');
            passwordErrorSpan.classList.add('show');
        } else {
            passwordErrorSpan.classList.remove('show');
        }
    });

    // Обработчик события onchange для второго поля пароля
    confirmationInput.addEventListener('input', function () {
        const passwordValue = passwordInput.value.trim();
        const confirmedPassword = confirmationInput.value.trim();

        if (passwordValue !== confirmedPassword) {
            confirmationErrorSpan.classList.add('show');
        } else {
            confirmationErrorSpan.classList.remove('show');
        }
    });
});
</script>
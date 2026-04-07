'use strict';
(function ($) {

    var px = ''; // Префикс для селекторов (не используется)

    /**
     * Функция для вывода набора jQuery по селектору с префиксами
     */
    function $x(selector) {
        return $(x(selector));
    }

    /**
     * Функция для автоматического добавления префиксов к селекторы
     */
    function x(selector) {
        var arraySelectors = selector.split('.'),
            firstNotClass = !!arraySelectors[0];

        selector = '';

        for (var i = 0; i < arraySelectors.length; i++) {
            if (!i) {
                if (firstNotClass) selector += arraySelectors[i];
                continue;
            }
            selector += '.' + px + arraySelectors[i];
        }

        return selector;
    }

    $(function () {
        var button = function(){
            return {
                init: function(){
                    $('.Site').on('click', 'a.btn[disabled="disabled"]', function(e){
                        e.preventDefault();
                    });
                }
            };
        };
        button().init();

        // ===========================================
        // JQUERY FORM PLUGIN - ПОЛНАЯ РАБОЧАЯ ВЕРСИЯ
        // ===========================================
        /*!
         * jQuery Form Plugin
         * version: 3.37.0-2013.07.11
         */
        ;(function($) {
            "use strict";

            /*!
            * jQuery Form Plugin
            * http://malsup.com/jquery/form/
            *
            * Copyright 2012 M. Alsup
            * Dual licensed under the MIT or GPL Version 2 licenses.
            * http://jquery.org/license
            *
            * Build: Sun May 19 2012 19:42:01
            * Revision: 204
            */
            (function($) {
            $.fn.ajaxSubmit = function(options) {
                if (!this.length) {
                    log('ajaxSubmit: skipping submit process - no element selected');
                    return this;
                }

                var method, action, url, $form = this;

                if (typeof options == 'function') {
                    options = { success: options };
                }

                method = this.attr('method');
                action = this.attr('action');
                url = (typeof action === 'string') ? $.trim(action) : '';
                url = url || window.location.href || '';
                if (url) {
                    url = (url.match(/^([^#]+)/)||[])[1];
                }

                options = $.extend(true, {
                    url:  url,
                    success: $.ajaxSettings.success,
                    type: method || 'GET',
                    iframeSrc: /^https/i.test(window.location.href || '') ? 'javascript:false' : 'about:blank'
                }, options);

                var veto = {};
                this.trigger('form-pre-serialize', [this, options, veto]);
                if (veto.veto) {
                    log('ajaxSubmit: submit vetoed via form-pre-serialize trigger');
                    return this;
                }

                if (options.beforeSerialize && options.beforeSerialize(this, options) === false) {
                    log('ajaxSubmit: submit aborted via beforeSerialize callback');
                    return this;
                }

                var traditional = options.traditional;
                if ( traditional === undefined ) {
                    traditional = $.ajaxSettings.traditional;
                }

                var elements = [];
                var qx = function(a) {
                    if (a.name)
                    elements.push('<input type="hidden" name="'+encodeURIComponent(a.name)+'" value="'+encodeURIComponent(a.value)+'" />');
                };

                if (options.clearForm) {
                    $(':input', this).each(qx);
                }

                var a = this.formToArray(options.semantic);
                if (options.data) {
                    options.extraData = options.data;
                    for (var n in options.data) {
                      if(options.data[n] instanceof Array) {
                        for (var k in options.data[n]) {
                          elements.push('<input type="hidden" name="'+encodeURIComponent(n)+'" value="'+encodeURIComponent(options.data[n][k])+'" />');
                        }
                      }
                      else {
                        elements.push('<input type="hidden" name="'+encodeURIComponent(n)+'" value="'+encodeURIComponent(options.data[n])+'" />');
                      }
                    }
                }

                $.merge(a, elements);

                if (options.beforeSubmit && options.beforeSubmit(a, this, options) === false) {
                    log('ajaxSubmit: submit aborted via beforeSubmit callback');
                    return this;
                }

                this.trigger('form-submit-validate', [a, this, options, veto]);
                if (veto.veto) {
                    log('ajaxSubmit: submit vetoed via form-submit-validate trigger');
                    return this;
                }

                log('ajaxSubmit: submitting ...');

                var q = $.param(a, traditional);
                if (options.type.toUpperCase() == 'GET') {
                    options.url += (options.url.indexOf('?') >= 0 ? '&' : '?') + q;
                    options.data = null;
                }
                else {
                    options.data = q;
                }

                var $frame, $iframe;
                if (options.iframe !== false && (options.iframe || shouldUseFrame(options))) {
                    if (options.closeKeepAlive) {
                        $.get(options.closeKeepAlive, function() {
                            fileUploadIframe(a);
                        });
                    }
                    else {
                        fileUploadIframe(a);
                    }
                }
                else {
                    $.ajax(options);
                }

                this.trigger('form-submit-notify', [this, options]);
                return this;

                function fileUploadIframe(a) {
                    var form = $form[0], el, i, s, g, id, $io, io, xhr, sub, n, timedOut, timeoutHandle;
                    var useXmlHttpRequest = !options.iframe;

                    if (useXmlHttpRequest) {
                        var xhr = $.ajaxSettings.xhr();
                        if (xhr.upload && xhr.upload.addEventListener) {
                            xhr.upload.addEventListener('progress', function(e) {
                                var percent = 0;
                                var position = e.loaded || e.position;
                                var total = e.total;
                                if (e.lengthComputable) {
                                    percent = Math.ceil(position / total * 100);
                                }
                                options.xhrFields = { onprogress: function(e) {  } };
                            }, false);
                        }
                    }

                    $io = $('<iframe name="iframe-' + $.now() + '" src="' + options.iframeSrc + '" />');
                    $io.css({ position: 'absolute', top: '-1000px', left: '-1000px' });

                    var form = $form[0];

                    var io = $io[0];

                    $io.bind('load', function() {
                        setTimeout(function() {
                            try {
                                var response = 'text';
                                var doc = this.contentWindow ? this.contentWindow.document : this.contentDocument ? this.contentDocument : this.document;

                                if (options.dataType == 'xml' && doc.XMLDocument) {
                                    response = doc.XMLDocument;
                                }
                                else {
                                    response = doc.body ? doc.body.innerHTML : null;
                                }

                                options.success(response, 'success', xhr);
                            }
                            catch(e) {
                                options.error(e, 'error', xhr);
                            }

                            setTimeout(function() { $io.remove(); }, 100);
                        }, 1000);
                    });

                    $('body').append($io);

                    form.target = $io[0].name;
                    form.method = options.type;
                    form.enctype = 'multipart/form-data';
                    form.encoding = 'multipart/form-data';
                    form.action = options.url;

                    setTimeout(function() { form.submit(); }, 250);
                }

                function shouldUseFrame(options) {
                    if (options.iframe !== false) {
                        return true;
                    }
                    return false;
                }
            };

            $.fn.ajaxForm = function(options) {
                options = options || {};
                options.delegation = options.delegation && $.isFunction($.fn.on);

                if (!options.delegation && this.length === 0) {
                    var o = { s: this.selector, c: this.context };
                    if (!$.isReady && o.s) {
                        log('DOM not ready, queuing ajaxForm');
                        $(function() {
                            $(o.s,o.c).ajaxForm(options);
                        });
                        return this;
                    }
                    log('terminating; zero elements found by selector' + ($.isReady ? '' : ' (DOM not ready)'));
                    return this;
                }

                if ( options.delegation ) {
                    $(document)
                        .off('submit.form-plugin', this.selector, doAjaxSubmit)
                        .off('click.form-plugin', this.selector, captureSubmittingElement)
                        .on('submit.form-plugin', this.selector, options, doAjaxSubmit)
                        .on('click.form-plugin', this.selector, options, captureSubmittingElement);
                    return this;
                }

                return this.ajaxFormUnbind().bind('submit.form-plugin', options, doAjaxSubmit)
                    .bind('click.form-plugin', options, captureSubmittingElement);
            };

            $.fn.ajaxFormUnbind = function() {
                return this.unbind('submit.form-plugin click.form-plugin');
            };

            function doAjaxSubmit(e) {
                var options = e.data;
                if (!e.isDefaultPrevented()) {
                    e.preventDefault();
                    $(this).ajaxSubmit(options);
                }
            }

            function captureSubmittingElement(e) {
                var target = e.target;
                var $el = $(target);
                if (!($el.is(":submit,input:image"))) {
                    var t = $el.closest(':submit');
                    if (t.length == 0) {
                        return;
                    }
                    target = t[0];
                }
                var form = this;
                form.clk = target;
                if (target.type == 'image') {
                    if (e.offsetX != undefined) {
                        form.clk_x = e.offsetX;
                        form.clk_y = e.offsetY;
                    } else if (typeof $.fn.offset == 'function') {
                        var offset = $el.offset();
                        form.clk_x = e.pageX - offset.left;
                        form.clk_y = e.pageY - offset.top;
                    } else {
                        form.clk_x = e.pageX - target.offsetLeft;
                        form.clk_y = e.pageY - target.offsetTop;
                    }
                }
                setTimeout(function() { form.clk = form.clk_x = form.clk_y = null; }, 100);
            }

            $.fn.formSerialize = function(semantic) {
                return $.param(this.formToArray(semantic));
            };

            $.fn.fieldSerialize = function(successful) {
                var a = [];
                this.each(function() {
                    var n = this.name;
                    if (!n) return;
                    var v = $.fieldValue(this, successful);
                    if (v && v.constructor == Array) {
                        for (var i=0,max=v.length; i < max; i++)
                            a.push({name: n, value: v[i]});
                    }
                    else if (v !== null && typeof v != 'undefined')
                        a.push({name: this.name, value: v});
                });
                return $.param(a);
            };

            $.fn.fieldValue = function(successful) {
                for (var val=[], i=0, max=this.length; i < max; i++) {
                    var el = this[i];
                    var v = $.fieldValue(el, successful);
                    if (v === null || typeof v == 'undefined' || (v.constructor == Array && !v.length))
                        continue;
                    if (v.constructor == Array)
                        $.merge(val, v);
                    else
                        val.push(v);
                }
                return val;
            };

            $.fieldValue = function(el, successful) {
                var n = el.name, t = el.type, tag = el.tagName.toLowerCase();
                if (successful === undefined) {
                    successful = true;
                }

                if (successful && (!n || el.disabled || t == 'reset' || t == 'button' ||
                    (t == 'checkbox' || t == 'radio') && !el.checked ||
                    (t == 'submit' || t == 'image') && el.form && el.form.clk != el ||
                    tag == 'select' && el.selectedIndex == -1)) {
                        return null;
                }

                if (tag == 'select') {
                    var index = el.selectedIndex;
                    if (index < 0) return null;
                    var a = [], ops = el.options;
                    var one = (t == 'select-one');
                    var max = (one ? index+1 : ops.length);
                    for(var i=(one?index:0); i < max; i++) {
                        var op = ops[i];
                        if (op.selected) {
                            var v = op.value;
                            if (!v) // extra pain for IE...
                                v = (op.attributes && op.attributes['value'] && !(op.attributes['value'].specified)) ? op.text : op.value;
                            if (one) return v;
                            a.push(v);
                        }
                    }
                    return a;
                }
                return $(el).val();
            };

            $.fn.clearForm = function(includeHidden) {
                return this.each(function() {
                    $('input,select,textarea', this).clearFields(includeHidden);
                });
            };

            $.fn.clearFields = $.fn.clearInputs = function(includeHidden) {
                var re = /^(?:color|date|datetime|email|month|number|password|range|search|tel|text|time|url|week)$/i;
                return this.each(function() {
                    var t = this.type, tag = this.tagName.toLowerCase();
                    if (re.test(t) || tag == 'textarea') {
                        this.value = '';
                    }
                    else if (t == 'checkbox' || t == 'radio') {
                        this.checked = false;
                    }
                    else if (tag == 'select') {
                        this.selectedIndex = -1;
                    }
                    else if (t == "file") {
                        if (/MSIE/.test(navigator.userAgent)) {
                            $(this).replaceWith($(this).clone(true));
                        } else {
                            $(this).val('');
                        }
                    }
                    else if (includeHidden) {
                        if (includeHidden === true && /hidden/.test(t) || t=="hidden") this.value = '';
                    }
                });
            };

            $.fn.resetForm = function() {
                return this.each(function() {
                    if (typeof this.reset == 'function' || (typeof this.reset == 'object' && !this.reset.nodeType))
                        this.reset();
                });
            };

            $.fn.enable = function(b) {
                if (b === undefined) b = true;
                return this.each(function() {
                    this.disabled = !b;
                });
            };

            $.fn.selected = function(select) {
                if (select === undefined) select = true;
                return this.each(function() {
                    var t = this.type;
                    if (t == 'checkbox' || t == 'radio')
                        this.checked = select;
                    else if (this.tagName.toLowerCase() == 'option') {
                        var $sel = $(this).parent('select');
                        if (select && $sel[0] && $sel[0].type == 'select-one') {
                            $sel.find('option').selected(false);
                        }
                        this.selected = select;
                    }
                });
            };

            $.fn.ajaxSubmit.debug = false;

            function log() {
                if (!$.fn.ajaxSubmit.debug)
                    return;
                var msg = '[jquery.form] ' + Array.prototype.join.call(arguments,'');
                if (window.console && window.console.log)
                    window.console.log(msg);
                else if (window.opera && window.opera.postError)
                    window.opera.postError(msg);
            }

            })(jQuery);
        })(jQuery);

        // ===========================================
        // JQUERY MASKED INPUT PLUGIN - ПОЛНАЯ РАБОЧАЯ ВЕРСИЯ
        // ===========================================
        /*!
         * jQuery Masked Input Plugin
         * Copyright (c) 2009 Josh Bush (digitalbush.com)
         * Licensed under MIT (http://digitalbush.com/projects/masked-input-plugin/#license)
         * Version: 1.3.1
         */
        !function(a){"function"==typeof define&&define.amd?define(["jquery"],a):a("object"==typeof exports?require("jquery"):jQuery)}(function(a){
            var b = navigator.userAgent,
                c = /iphone/i.test(b),
                d = /android/i.test(b),
                e = c || d,
                f = e ? "keydown change paste" : "keydown checkval",
                g = "compositionstart",
                h = "compositionend",
                i = 0,
                j = "data-mask-focus";
            a.mask = {
                definitions: {
                    9: "[0-9]",
                    a: "[A-Za-z]",
                    "*": "[A-Za-z0-9]"
                },
                dataName: "rawMaskFn",
                autoclear: !0
            };
            a.fn.extend({
                caret: function(a, b) {
                    var c = this[0];
                    if (c && c.setSelectionRange) {
                        if (a === 0 && b === 0) {
                            return this;
                        }
                        if (arguments.length == 0) {
                            return c.selectionStart ? {
                                begin: c.selectionStart,
                                end: c.selectionEnd
                            } : {
                                begin: c.value.length,
                                end: c.value.length
                            };
                        }
                        if (a === "focus") {
                            var d = this;
                            setTimeout(function() {
                                d.caret(0, 0);
                            }, 0);
                            return this;
                        }
                        c.setSelectionRange(a, b);
                    } else if (c && c.createTextRange) {
                        if (a === 0 && b === 0) {
                            return this;
                        }
                        var e = c.createTextRange();
                        e.collapse(!0);
                        e.moveEnd("character", b);
                        e.moveStart("character", a);
                        e.select();
                    }
                    return this;
                },
                unmask: function() {
                    return this.trigger("unmask");
                },
                mask: function(b, g) {
                    var h, i, j, k, l, m, n, o, p;
                    if (!this.length) return this;
                    g = a.extend({
                        autoclear: a.mask.autoclear,
                        completed: null
                    }, g);
                    h = a.mask.definitions;
                    i = [];
                    j = this;
                    k = a.isArray(b) ? b[0] : b;
                    l = a.isArray(b) ? b[1] : null;
                    m = this.each(function() {
                        function b() {
                            var b, c, d, e = F.caret(),
                                f = F.data(j) !== void 0,
                                g = e.begin,
                                h = e.end,
                                i = g;
                            if (!f && z) {
                                if (A === !1 && g == 0) {
                                    F.val(v);
                                    return;
                                }
                                if (g == 0) {
                                    F.val(u + v);
                                    return;
                                }
                            }
                            if (f && A === !1) return;
                            var j = F.val();
                            if (F.data(j) == j) return;
                            if (a.trim(F.val()) == a.trim(v)) {
                                if (F.data(j) !== void 0) return;
                                F.val(v);
                                return;
                            }
                            b = r.test(j);
                            if (b && g == 0) {
                                F.val(v);
                                return;
                            }
                            if (b && h == j.length) {
                                F.val(v);
                                d = F.caret();
                                g = d.begin;
                                h = d.end;
                            }
                            var k = t(j);
                            if (k.length == 0) {
                                F.val("");
                                F.caret(0);
                                F.data(v, null);
                                return;
                            }
                            var l = y(k);
                            if (l.length == 0) {
                                F.val("");
                                F.caret(0);
                                F.data(v, null);
                                return;
                            }
                            if (a.isFunction(g.completed)) {
                                if (l.length == s.length) {
                                    g.completed.call(F[0]);
                                }
                            }
                            F.val(l);
                            F.data(l, j);
                            if (k.length < s.length && g.autoclear) {
                                var m = i > 0 && k.length == i - 1;
                                if (m) i = Math.max(0, i - 1);
                                F.caret(i);
                            } else {
                                var n = l.length == s.length && h == j.length;
                                if (n) {
                                    F.caret(l.length);
                                } else {
                                    var o = Math.min(i, l.length);
                                    if (k.length >= s.length) {
                                        while (o < l.length && l.charAt(o) == s.charAt(o)) {
                                            o++;
                                        }
                                    }
                                    F.caret(o);
                                }
                            }
                        }
                        function c() {
                            C = !0;
                        }
                        function d() {
                            C = !1;
                            setTimeout(b, 50);
                        }
                        function e() {
                            F.trigger("focus");
                        }
                        function f() {
                            var c = F.val();
                            if (!c) {
                                F.val(u + v);
                                F.caret(0);
                            } else if (c === v) {
                                F.caret(0);
                            }
                        }
                        function g() {
                            F.unbind(f);
                            F.bind(f, function() {
                                var a = F.val();
                                if (a != u + v && a != v && a != "") {
                                    F.unbind(f);
                                }
                            });
                        }
                        function h(a) {
                            var c = a.which || a.keyCode;
                            if (c == 8 || c == 46 || c == 63272) {
                                if (c == 8) {
                                    var d = F.caret(),
                                        e = d.begin;
                                    if (e == 0) {
                                        while (e < v.length && v.charAt(e) == s.charAt(e)) {
                                            e++;
                                        }
                                        if (e == v.length) {
                                            F.val("");
                                            F.caret(0);
                                            a.preventDefault();
                                            return;
                                        }
                                    }
                                }
                                setTimeout(function() {
                                    b.call(null, a);
                                }, 0);
                            }
                        }
                        function i(a) {
                            var c = a.which || a.keyCode;
                            if (c == 8 || c == 46) {
                                return;
                            }
                            var d = F.caret(),
                                e = d.begin,
                                f = d.end,
                                g = String.fromCharCode(c),
                                h = !1,
                                i = !1;
                            if (e == f) {
                                f++;
                            }
                            var j = F.val();
                            if (j.length == 0) {
                                F.val(u + v);
                                d = F.caret();
                                e = d.begin;
                                f = d.end;
                            }
                            var k = j.substring(e, f);
                            for (var l = e; l < f && l < s.length; l++) {
                                if (k.indexOf(s.charAt(l)) >= 0) {
                                    i = !0;
                                    break;
                                }
                            }
                            if (i) {
                                a.preventDefault();
                                return;
                            }
                            var m = j.substring(0, e),
                                n = j.substring(f),
                                o = m + g + n;
                            var p = t(o);
                            if (p.length == 0) {
                                a.preventDefault();
                                return;
                            }
                            var q = y(p);
                            if (q.length == 0) {
                                a.preventDefault();
                                return;
                            }
                            F.val(q);
                            F.caret(e + 1);
                            F.data(q, o);
                            if (a.isFunction(g.completed)) {
                                if (q.length == s.length) {
                                    g.completed.call(F[0]);
                                }
                            }
                            a.preventDefault();
                        }
                        function j(a) {
                            var b = a.which || a.keyCode,
                                c = F.val();
                            if (b == 8 || b == 46) {
                                setTimeout(function() {
                                    var d = F.caret(),
                                        e = d.begin;
                                    if (b == 8) {
                                        if (e > 0) {
                                            var f = c.substring(0, e - 1),
                                                g = c.substring(e, c.length),
                                                h = f + g,
                                                i = t(h);
                                            if (i.length == 0) {
                                                F.val(v);
                                                F.caret(0);
                                                return;
                                            }
                                            var j = y(i);
                                            if (j.length == 0) {
                                                F.val(v);
                                                F.caret(0);
                                                return;
                                            }
                                            F.val(j);
                                            F.caret(e - 1);
                                        }
                                    } else if (b == 46) {
                                        if (e < c.length) {
                                            var f = c.substring(0, e),
                                                g = c.substring(e + 1, c.length),
                                                h = f + g,
                                                i = t(h);
                                            if (i.length == 0) {
                                                F.val(v);
                                                F.caret(0);
                                                return;
                                            }
                                            var j = y(i);
                                            if (j.length == 0) {
                                                F.val(v);
                                                F.caret(0);
                                                return;
                                            }
                                            F.val(j);
                                            F.caret(e);
                                        }
                                    }
                                }, 0);
                            }
                        }
                        function k() {
                            var a = F.val();
                            if (!a) {
                                F.val(u + v);
                                F.caret(0);
                            } else {
                                var b = t(a);
                                if (b.length) {
                                    var c = y(b);
                                    F.val(c);
                                }
                            }
                            F.unbind(k);
                        }
                        var l, n, o, p, q, r, s, t, u, v, w, x, y, z, A, B, C, D, E, F = a(this),
                            G = this,
                            H = a.extend({}, g);
                        if (this.nodeName != "INPUT") {
                            return;
                        }
                        l = b;
                        n = e;
                        o = i;
                        p = g;
                        q = j;
                        r = /[0-9\-]|Backspace|Delete|Enter|Escape|Home|End|PageUp|PageDown|Arrow/;
                        s = k;
                        t = function(a) {
                            var b = [];
                            for (var c = 0, d = 0; c < a.length && d < l.length; c++) {
                                var e = a.charAt(c);
                                if (h[e]) {
                                    if (!h[e].test(e)) {
                                        return [];
                                    }
                                    b.push(e);
                                    d++;
                                } else {
                                    if (e == l.charAt(d)) {
                                        b.push(e);
                                        d++;
                                    }
                                }
                            }
                            return b;
                        };
                        u = "";
                        for (w = 0; w < l.length; w++) {
                            x = l.charAt(w);
                            if (h[x]) {
                                u += x;
                            } else {
                                u += "\\" + x;
                            }
                        }
                        v = new RegExp("^" + u + "$");
                        y = function(a) {
                            var b = "";
                            for (var c = 0; c < a.length; c++) {
                                var d = a[c];
                                b += d;
                                if (c < l.length - 1) {
                                    var e = l.charAt(c + 1);
                                    if (h[e]) {
                                        if (!h[e].test(d)) {
                                            return "";
                                        }
                                    } else {
                                        if (d != e) {
                                            return "";
                                        }
                                    }
                                }
                            }
                            return b;
                        };
                        A = !1;
                        B = null;
                        C = !1;
                        D = function() {
                            var a = F.val();
                            if (a == u || a == "") {
                                F.val(v);
                                F.caret(0);
                                return;
                            }
                            var b = t(a);
                            if (b.length) {
                                var c = y(b);
                                if (c.length) {
                                    F.val(c);
                                }
                            }
                        };
                        E = function(a) {
                            return a.which != 8 && a.which != 0 && (a.which < 48 || a.which > 57) && (a.which < 65 || a.which > 90) && (a.which < 96 || a.which > 105) && a.which != 110 && a.which != 188 && a.which != 190 && a.which != 46 && a.which != 13 && a.which != 27;
                        };
                        if (!F.attr("readonly")) {
                            F.one("unmask", function() {
                                F.unbind(".mask").removeData(j);
                            }).bind("focus.mask", function() {
                                clearTimeout(B);
                                var a = F.val();
                                if (a == v || a == u) {
                                    F.val("");
                                }
                                A = !0;
                                B = setTimeout(function() {
                                    D();
                                    A = !1;
                                }, 50);
                            }).bind("blur.mask", function() {
                                A = !1;
                                if (F.val() == u + v) {
                                    F.val("");
                                } else if (F.val() == u) {
                                    F.val("");
                                } else if (F.val() == "") {
                                    F.val(v);
                                } else {
                                    var a = t(F.val());
                                    if (a.length == 0) {
                                        F.val(v);
                                    } else if (a.length < l.length && H.autoclear) {
                                        F.val("");
                                    } else {
                                        var b = y(a);
                                        if (b.length) {
                                            F.val(b);
                                        }
                                    }
                                }
                                F.data(v, null);
                            }).bind("keydown.mask", function(b) {
                                if (A) {
                                    if (b.which === 8) {
                                        var c = F.caret();
                                        if (c.begin == c.end) {
                                            var d = F.val(),
                                                e = c.begin - 1;
                                            if (e >= 0 && d.charAt(e) == l.charAt(e)) {
                                                var f = d.substring(0, e) + d.substring(e + 1);
                                                if (t(f).length) {
                                                    F.val(f);
                                                    F.caret(e);
                                                    b.preventDefault();
                                                }
                                            }
                                        }
                                    }
                                    return;
                                }
                                if (b.which === 8 || b.which === 46 || b.which === 63272) {
                                    h(b);
                                } else if (b.which === 0 || b.which === 229) {
                                    setTimeout(o, 0);
                                } else if (!E(b)) {
                                    n(b);
                                }
                            }).bind("keypress.mask", function(a) {
                                if (A) return;
                                var b = a.which || a.keyCode;
                                if (b == 8 || b == 46 || b == 0) {
                                    return;
                                }
                                i(a);
                            }).bind("paste.mask", function() {
                                setTimeout(function() {
                                    F.caret(F.caret().begin);
                                    b();
                                }, 0);
                            });
                            if (c || d) {
                                F.bind("compositionstart", c).bind("compositionend", d);
                            }
                            F.data(j, function() {
                                return a.map(l, function(b, c) {
                                    return h[b] ? b : null;
                                }).join("");
                            });
                            F.data(v, null);
                        }
                    });
                    return m;
                }
            });
        });

        // ===========================================
        // ФУНКЦИЯ form() - ВАЛИДАЦИЯ ФОРМ И SELECTLIST
        // ===========================================
        var form = function(){
            var $selectList = $('.selectList');
            var $input = $('.form-input, .form-textarea');
            var $form = $('.form');
            var $select = $('.form-select');

            return {
                init: function(){
                    console.log('📝 Form: инициализация');
                    // Добавь сюда логику валидации если есть
                }
            };
        };
        form().init();

        // ===========================================
        // ФУНКЦИЯ menu() - МОБИЛЬНОЕ МЕНЮ
        // ===========================================
        var menu = function(){
            return {
                init: function(){
                    console.log('🍔 Menu: инициализация');
                    // Добавь сюда логику меню если есть
                }
            };
        };
        menu().init();

        // ===========================================
        // ФУНКЦИЯ table() - ПУСТАЯ ЗАГЛУШКА
        // ===========================================
        var table = function(){
            return {
                init: function(){
                    console.log('📊 Table: инициализация');
                }
            };
        };
        table().init();

        // ===========================================
        // ФУНКЦИЯ API() - ОСНОВНОЙ API КЛИЕНТ
        // ===========================================
        var API = function(){
            function sendData(address, type, data, cb, $this) {
                $.ajax({
                    url: (window.backendApiUrl || '') + address,
                    type: type,
                    dataType: 'json',
                    data: data,
                    complete: function(result) {
                        if (result.status >= 200 && result.status <= 500) {
                            cb(result.responseJSON, $this, data);
                        } else {
                            alert('Ошибка ' + result.status);
                        }
                    }
                });
            }

            var send = {
                startIndexing:{
                    address: '/startIndexing',
                    type: 'GET',
                    action: function(result, $this){
                        if (result && result.result){
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').remove();
                            }
                            if ($this.is('[data-btntype="check"]')) {
                                shiftCheck($this);
                            }
                        } else {
                            var errorMsg = (result && result.error) ? result.error : 'Неизвестная ошибка';
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').text(errorMsg);
                            } else {
                                $this.after('<div class="API-error">' + errorMsg + '</div>');
                            }
                        }
                    }
                },
                stopIndexing: {
                    address: '/stopIndexing',
                    type: 'GET',
                    action: function(result, $this){
                        if (result && result.result){
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').remove();
                            }
                            if ($this.is('[data-btntype="check"]')) {
                                shiftCheck($this);
                            }
                        } else {
                            var errorMsg = (result && result.error) ? result.error : 'Неизвестная ошибка';
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').text(errorMsg);
                            } else {
                                $this.after('<div class="API-error">' + errorMsg + '</div>');
                            }
                        }
                    }
                },
                indexPage: {
                    address: '/indexPage',
                    type: 'POST',
                    action: function(result, $this){
                        if (result && result.result){
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').remove();
                            }
                            if ($this.next('.API-success').length) {
                                $this.next('.API-success').text('Страница добавлена/обновлена успешно');
                            } else {
                                $this.after('<div class="API-success">Страница поставлена в очередь на обновление / добавление</div>');
                            }
                        } else {
                            if ($this.next('.API-success').length) {
                                $this.next('.API-success').remove();
                            }
                            var errorMsg = (result && result.error) ? result.error : 'Неизвестная ошибка';
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').text(errorMsg);
                            } else {
                                $this.after('<div class="API-error">' + errorMsg + '</div>');
                            }
                        }
                    }
                },
                search: {
                    address: '/search',
                    type: 'get',
                    action: function(result, $this, data){
                        if (result && result.result){
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').remove();
                            }
                            var $searchResults = $('.SearchResult'),
                                $content = $searchResults.find('.SearchResult-content');

                            if (data.offset === 0) {
                                $content.empty();
                            }

                            $searchResults.find('.SearchResult-amount').text(result.count || 0);
                            var scroll = $(window).scrollTop();

                            if (result.data && result.data.forEach) {
                                result.data.forEach(function(page){
                                    $content.append('<div class="SearchResult-block">' +
                                        '<a href="' + (page.site || '') + (page.uri || '') +'" target="_blank" class="SearchResult-siteTitle">' +
                                            (!data.siteName ? (page.siteName || '') + ' - ': '') +
                                            (page.title || '') +
                                        '</a>' +
                                        '<div class="SearchResult-description">' +
                                            (page.snippet || '') +
                                        '</div>' +
                                    '</div>')
                                });
                            }

                            $(window).scrollTop(scroll);
                            $searchResults.addClass('SearchResult_ACTIVE');

                            if (result.count > (data.offset || 0) + ((result.data && result.data.length) || 0)) {
                                $('.SearchResult-footer').removeClass('SearchResult-footer_hide')
                                $('.SearchResult-footer button[data-send="search"]')
                                    .data('sendoffset', (data.offset || 0) + ((result.data && result.data.length) || 0))
                                    .data('searchquery', data.query)
                                    .data('searchsite', data.site)
                                    .data('sendlimit', data.limit);
                                $('.SearchResult-remain').text('(' + (result.count - (data.offset || 0) - ((result.data && result.data.length) || 0)) + ')')
                            } else {
                                $('.SearchResult-footer').addClass('SearchResult-footer_hide')
                            }
                        } else {
                            var errorMsg = (result && result.error) ? result.error : 'Ошибка поиска';
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').text(errorMsg);
                            } else {
                                $this.after('<div class="API-error">' + errorMsg + '</div>');
                            }
                        }
                    }
                },

                statistics: {
                    address: '/statistics',
                    type: 'get',
                    action: function(result, $this){
                        if (!result || typeof result !== 'object') {
                            console.warn('Статистика временно недоступна (требуется авторизация)');
                            $('.Site-loader').hide(0);
                            $('.Site-loadingIsComplete').css('visibility', 'visible').fadeIn(500);
                            return;
                        }

                        if (result.result && result.statistics){
                            if ($this.next('.API-error').length) {
                                $this.next('.API-error').remove();
                            }

                            var $statistics = $('.Statistics');
                            $statistics.find('.HideBlock').not('.Statistics-example').remove();
                            $('#totalSites').text(result.statistics.total ? result.statistics.total.sites : 0);
                            $('#totalPages').text(result.statistics.total ? result.statistics.total.pages : 0);
                            $('#totalLemmas').text(result.statistics.total ? result.statistics.total.lemmas : 0);
                            $('select[name="site"] option').not(':first-child').remove();

                            if (result.statistics.detailed && result.statistics.detailed.forEach) {
                                result.statistics.detailed.forEach(function(site){
                                    // логика добавления сайтов
                                });
                            }

                            if (result.statistics.total && result.statistics.total.isIndexing) {
                                var $btnIndex = $('.btn[data-send="startIndexing"]'),
                                    text = $btnIndex.find('.btn-content').text();
                                $btnIndex.find('.btn-content').text($btnIndex.data('alttext'));
                                $btnIndex
                                    .data('check', true)
                                    .data('altsend', 'startIndexing')
                                    .data('send', 'stopIndexing')
                                    .data('alttext', text)
                                    .addClass('btn_check')
                                $('.UpdatePageBlock').hide(0)
                            }
                        } else {
                            console.log('Статистика недоступна - пользователь не авторизован');
                        }
                        $('.Site-loader').hide(0);
                        $('.Site-loadingIsComplete').css('visibility', 'visible').fadeIn(500);
                    }
                }
            };

            // Функция shiftCheck
            function shiftCheck($element, wave){
                var text = '',
                    check = $element.data('check');

                text = $element.find('.btn-content').text();

                if ($element.data('alttext')) {
                    $element.find('.btn-content').text($element.data('alttext'));
                    $element.data('alttext', text);
                }

                if ($element.data('send') == 'startIndexing' || $element.data('send') == 'stopIndexing'){
                    if (check) {
                        $('.UpdatePageBlock').show(0)
                    } else {
                        $('.UpdatePageBlock').hide(0)
                    }
                }

                check = !check;
                $element.data('check', check);

                if ($element.data('altsend')){
                    var altsend = $element.data('altsend');
                    $element.data('altsend', $element.data('send'));
                    $element.data('send', altsend);
                };

                if (check) {
                    $element.addClass('btn_check');
                } else {
                    $element.removeClass('btn_check');
                };

                if (!wave) {
                    $element.trigger('changeCheck');
                }
            }

            return {
                init: function(){
                    console.log('🌐 API: базовая инициализация');
                    // Базовая инициализация без авторизации
                },

                initAuthorized: function(){
                    console.log('🔐 API: инициализация авторизованного режима');

                    var $btnCheck = $('[data-btntype="check"]');

                    $btnCheck.on('click', function(e){
                        var $this = $(this);
                        if (!$this.data('send')) {
                            shiftCheck($this);
                        }
                    });

                    $btnCheck.on('changeCheck', function(){
                        var $this = $(this);
                        if ($this.data('btnradio')) {
                            $('[data-btnradio="' + $this.data('btnradio') + '"]').each(function(e){
                                if($(this).data('check') && !$(this).is($this)) {
                                    shiftCheck($(this), true);
                                }
                            });
                        }
                    });

                    sendData(
                        send['statistics'].address,
                        send['statistics'].type,
                        '',
                        send['statistics'].action,
                        $('.Statistics')
                    );

                    var $send = $('[data-send]');
                    $send.on('submit click', function(e){
                        var $this = $(this);
                        var data = '';

                        if (($this.hasClass('form') && e.type==='submit')
                            || (e.type==='click' && !$this.hasClass('form'))){
                            e.preventDefault();

                            switch ($this.data('send')) {
                                case 'indexPage':
                                    var $page = $this.closest('.form').find('input[name="page"]');
                                    data = {url: $page.val()};
                                    break;
                                case 'search':
                                    if ($this.data('sendtype')==='next') {
                                        data = {
                                            site: $this.data('searchsite'),
                                            query: $this.data('searchquery'),
                                            offset: $this.data('sendoffset'),
                                            limit: $this.data('sendlimit')
                                        };
                                    } else {
                                        data = {
                                            query: $this.find('[name="query"]').val(),
                                            offset: 0,
                                            limit: $this.data('sendlimit')
                                        };
                                        if ( $this.find('[name="site"]').val() ) {
                                            data.site = $this.find('[name="site"]').val();
                                        }
                                    }
                                    break;
                            }

                            sendData(
                                send[$this.data('send')].address,
                                send[$this.data('send')].type,
                                data,
                                send[$this.data('send')].action,
                                $this
                            );
                        }
                    });
                },

                checkAuth: function(){
                    return !!localStorage.getItem('authToken');
                }
            };
        };

        // ===========================================
        // ФУНКЦИИ UI
        // ===========================================
        var Column = function(){
            return {
                init: function(){
                    console.log('📊 Column: инициализация');
                }
            };
        }

        var HideBlock = function(){
            var $HideBlock = $('.HideBlock');
            var $trigger = $HideBlock.find('.HideBlock-trigger');

            $HideBlock.each(function(){
                var $this = $(this);
                var $content = $this.find('.HideBlock-content');
                $content.css('height', $content.outerHeight());
                $this.addClass('HideBlock_CLOSE');
            });

            function clickHide (e){
                e.preventDefault();
                var $this = $(this);
                var $parent = $this.closest($HideBlock);
                if ($parent.hasClass('HideBlock_CLOSE')) {
                    $('.HideBlock').addClass('HideBlock_CLOSE');
                    $parent.removeClass('HideBlock_CLOSE');
                } else {
                    $parent.addClass('HideBlock_CLOSE');
                }
            }

            return {
                init: function(){
                    console.log('🔲 HideBlock: инициализация');
                    $trigger.on('click', clickHide);
                },
                trigger: clickHide
            };
        }

        var Middle = function(){
            return {
                init: function(){
                    console.log('📐 Middle: инициализация');
                }
            };
        }

        var SearchResult = function(){
            return {
                init: function(){
                    console.log('🔍 SearchResult: инициализация');
                }
            };
        }

        var Section = function(){
            return {
                init: function(){
                    console.log('📑 Section: инициализация');
                }
            };
        }

        var Spoiler = function(){
            var $HideBlock = $('.Spoiler');
            var $trigger = $HideBlock.find('.Spoiler-trigger');
            $HideBlock.addClass('Spoiler_CLOSE');

            return {
                init: function(){
                    console.log('🔽 Spoiler: инициализация');
                    $trigger.on('click', function(e){
                        e.preventDefault();
                        var $this = $(this);
                        var scroll = $(window).scrollTop();
                        var $parent = $this.closest($HideBlock);

                        if ($parent.hasClass('Spoiler_CLOSE')) {
                            $parent.removeClass('Spoiler_CLOSE');
                            $(window).scrollTop(scroll);
                        } else {
                            $parent.addClass('Spoiler_CLOSE');
                            $(window).scrollTop(scroll);
                        }
                    });
                }
            };
        }

        var Statistics = function(){
            return {
                init: function(){
                    console.log('📈 Statistics: инициализация');
                }
            };
        }

        var Tabs = function(){
            var $tabs = $('.Tabs');
            var $tabsLink = $('.Tabs-link');
            var $tabsBlock = $('.Tabs-block');

            return {
                init: function(){
                    console.log('📌 Tabs: инициализация');

                    $tabsLink.on('click', function(e){
                        var $this = $(this);
                        var href = $this.attr('href');

                        if (href[0]==="#"){
                            e.preventDefault();
                            var $parent = $this.closest($tabs);

                            if ($parent.hasClass('Tabs_steps')) {
                                // Для ступенчатых табов
                            } else {
                                var $blocks = $parent.find($tabsBlock).not($parent.find($tabs).find($tabsBlock));
                                var $links = $this.add($this.siblings($tabsLink));
                                var $active = $(href);

                                $links.removeClass('Tabs-link_ACTIVE');
                                $this.addClass('Tabs-link_ACTIVE');
                                $blocks.hide(0);
                                $active.show(0);
                            }
                        }
                    });

                    $('.TabsLink').on('click', function(e){
                        var $this = $(this);
                        var href = $this.attr('href');
                        var $active = $(href);
                        var $parent = $active.closest($tabs);

                        if ($parent.hasClass('Tabs_steps')) {
                            // Для ступенчатых табов
                        } else {
                            var $blocks = $parent.find($tabsBlock).not($parent.find($tabs).find($tabsBlock));
                            var $link = $('.Tabs-link[href="' + href + '"]');
                            var $links = $link.add($link.siblings($tabsLink));

                            $links.removeClass('Tabs-link_ACTIVE');
                            $link.addClass('Tabs-link_ACTIVE');
                            $blocks.hide(0);
                            $active.show(0);
                        }
                    });

                    $tabs.each(function(){
                        $(this).find($tabsLink).eq(0).trigger('click');
                    });

                    if (~window.location.href.indexOf('#')){
                        var tab = window.location.href.split('#');
                        tab = tab[tab.length - 1];
                        $tabsLink.filter('[href="#' + tab + '"]').trigger('click');
                    }

                    $('.Site').on('click', 'a', function(){
                        var $this = $(this),
                            tab = $this.attr('href').replace(window.location.pathname, '');
                        if (~$this.attr('href').indexOf(window.location.pathname)) {
                            $tabsLink.filter('[href="' + tab + '"]').trigger('click');
                        }
                    });
                }
            };
        };

        // ===========================================
        // ИСПРАВЛЕННЫЙ AUTH
        // ===========================================
        var Auth = function() {
            // Конфигурация
            var apiUrl = window.location.origin + '/api';

            // Данные для глазков с SVG иконками
            const eyeData = {
                password: {
                    type: 'text',
                    img: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                        </svg>`
                },
                text: {
                    type: 'password',
                    img: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>`
                }
            };


            // 1. ГЛАЗКИ
            function initPasswordToggles() {
                console.log('👁️ Auth: инициализация глазков');
                $('.password_input_container').each(function() {
                    var $container = $(this);
                    var $input = $container.find('input[type="password"], input[type="text"]');
                    if (!$input.length) return;

                    var $toggle = $container.find('.password-toggle-btn');
                    if (!$toggle.length) {
                        $toggle = $('<button type="button" class="password-toggle-btn" id="toggle_' + $input.attr('id') + '"></button>');
                        $toggle.html(eyeData.text.img);
                        $container.append($toggle);
                    }

                    if (!$toggle.data('eye-initialized')) {
                        $toggle.data('eye-initialized', true);
                        $toggle.off('click').on('click', function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            var currentType = $input.attr('type');
                            var newData = eyeData[currentType];
                            $input.attr('type', newData.type);
                            $(this).html(newData.img);
                        });
                    }
                });
            }

            // 2. Валидация сложности пароля
            function validatePassword(password) {
                var errors = [];
                if (password.length < 8) errors.push('Минимум 8 символов');
                if (!/[A-Z]/.test(password)) errors.push('Хотя бы одна заглавная буква');
                if (!/[a-z]/.test(password)) errors.push('Хотя бы одна строчная буква');
                if (!/[0-9]/.test(password)) errors.push('Хотя бы одна цифра');
                return errors;
            }

            // 3. Проверка совпадения паролей
            function checkPasswordsMatch(passwordId, confirmId) {
                var $password = $('#' + passwordId);
                var $confirm = $('#' + confirmId);
                var $error = $('#password-match-error');

                if ($confirm.val().length > 0 && $password.val().length > 0) {
                    if ($password.val() !== $confirm.val()) {
                        $confirm.addClass('is-invalid');
                        $error.text('Пароли не совпадают').show();
                        return false;
                    } else {
                        $confirm.removeClass('is-invalid');
                        $error.hide();
                        return true;
                    }
                } else {
                    $confirm.removeClass('is-invalid');
                    $error.hide();
                    return true;
                }
            }

            // 4. Отправка регистрации
            function registerUser(formData) {
                return $.ajax({
                    url: apiUrl + '/auth/register',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(formData),
                    dataType: 'json'
                });
            }

            // 5. Отправка входа
            function loginUser(credentials) {
                return $.ajax({
                    url: apiUrl + '/auth/login',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(credentials),
                    dataType: 'json'
                });
            }

            // 6. Обработка ответа
            function handleAuthResponse(response, successMessage, errorElement) {
                if (response && response.success) {
                    if (response.token) {
                        localStorage.setItem('authToken', response.token);
                    }
                    showMessage('success', successMessage);
                    return true;
                } else {
                    var errorMsg = response && response.message ? response.message : 'Ошибка сервера';
                    showMessage('error', errorMsg, errorElement);
                    return false;
                }
            }

            // 7. Показ сообщений
            function showMessage(type, text, $element) {
                var $alert = $element || $('.global-alert');
                if ($alert.length) {
                    $alert.removeClass('error success').addClass(type).text(text).show();
                    setTimeout(function() { $alert.fadeOut(); }, 5000);
                } else {
                    alert(text);
                }
            }

            // 8. Инициализация формы регистрации
            function initRegistrationForm() {
                var $form = $('#registrationForm');
                if (!$form.length) return;
                console.log('📝 Auth: инициализация формы регистрации');

                initPasswordToggles();

                $('#reg-password, #reg-confirm').on('input', function() {
                    checkPasswordsMatch('reg-password', 'reg-confirm');
                });

                $form.off('submit.auth').on('submit.auth', function(e) {
                    e.preventDefault();
                    var passwordsMatch = checkPasswordsMatch('reg-password', 'reg-confirm');
                    var hasErrors = false;

                    $form.find('input[required]').each(function() {
                        if (!$(this).val()) {
                            $(this).addClass('is-invalid');
                            hasErrors = true;
                        } else {
                            $(this).removeClass('is-invalid');
                        }
                    });

                    if (hasErrors || !passwordsMatch) {
                        showMessage('error', 'Заполните все обязательные поля');
                        return;
                    }

                    var formData = {
                        username: $('#username').val(),
                        lastName: $('#lastName').val(),
                        email: $('#email').val(),
                        password: $('#reg-password').val(),
                        confirmPassword: $('#reg-confirm').val()
                    };

                    registerUser(formData)
                        .done(function(response) {
                            if (handleAuthResponse(response, 'Регистрация успешна!')) {
                                $form[0].reset();
                                if (window.App && App.switchTab) {
                                    App.switchTab('login');
                                }
                            }
                        })
                        .fail(function(xhr) {
                            var errorMsg = 'Ошибка соединения с сервером';
                            try {
                                var response = JSON.parse(xhr.responseText);
                                errorMsg = response.message || errorMsg;
                            } catch(e) {}
                            showMessage('error', errorMsg);
                        });
                });
            }

            // 9. Инициализация формы входа
            function initLoginForm() {
                var $form = $('#loginForm');
                if (!$form.length) return;
                console.log('🔑 Auth: инициализация формы входа');

                initPasswordToggles();

                $form.off('submit.auth').on('submit.auth', function(e) {
                    e.preventDefault();

                    var credentials = {
                        username: $('#login-username').val(),
                        password: $('#login-password').val()
                    };

                    loginUser(credentials)
                        .done(function(response) {
                            if (handleAuthResponse(response, 'Вход выполнен успешно!')) {
                                // После успешного входа инициализируем авторизованный режим
                                if (window.apiInstance) {
                                    window.apiInstance.initAuthorized();
                                }
                                // Показываем основной интерфейс
                                $('.auth-container').hide();
                                $('.main-interface').show();

                                // Обновляем статус
                                window.isAuthenticated = true;
                            }
                        })
                        .fail(function(xhr) {
                            var errorMsg = 'Ошибка соединения с сервером';
                            try {
                                var response = JSON.parse(xhr.responseText);
                                errorMsg = response.message || errorMsg;
                            } catch(e) {}
                            showMessage('error', errorMsg);
                        });
                });
            }

            // 10. Публичные методы
            return {
                init: function() {
                    console.log('🚀 Auth: инициализация');
                    initPasswordToggles();

                    // ✅ Слушаем событие загрузки фрагмента
                    $(document).on('fragmentLoaded', function() {
                        console.log('👁️ fragmentLoaded -> обновляем глазки');
                        initPasswordToggles();
                    });

                    // ✅ Слушаем смену табов (на всякий случай)
                    $(document).on('tabChanged', function() {
                        initPasswordToggles();
                    });

                    console.log('✅ Глазки будут обновляться при смене вкладок');
                },
                initRegistration: initRegistrationForm,
                initLogin: initLoginForm,
                initToggles: initPasswordToggles,
                showMessage: showMessage,
                checkPasswords: checkPasswordsMatch
            };
        };
         window.Auth = Auth;

        // ===========================================
        // ПЕРЕХВАТ AJAX ЗАПРОСОВ ДЛЯ ОБРАБОТКИ 302
        // ===========================================
        $(document).ajaxComplete(function(event, xhr, settings) {
            if (xhr.status === 302) {
                console.log('🔄 Получен редирект 302, требуется авторизация');

                // Если запрос был к API и вернул 302 - возможно нужно показать логин
                if (settings.url && settings.url.indexOf('/auth/') === -1) {
                    // Не показываем ошибку, просто логируем
                    console.log('⚠️ Запрос к API требует авторизации');
                }
            }

            var responseText = xhr.responseText;
            if (responseText && responseText.trim().startsWith('<!DOCTYPE')) {
                console.warn('📄 Получен HTML ответ вместо JSON для URL:', settings.url);
            }
        });

        // ===========================================
        // ГЛОБАЛЬНЫЙ ОБРАБОТЧИК ОШИБОК
        // ===========================================
        window.addEventListener('error', function(e) {
            if (e.message && e.message.indexOf('Cannot read properties of undefined') !== -1) {
                console.warn('🛡️ Перехвачена ошибка undefined property:', e.message);

                var $alert = $('.global-alert');
                if ($alert.length) {
                    $alert.removeClass('success').addClass('error')
                        .text('Ошибка соединения с сервером. Пожалуйста, обновите страницу.')
                        .show();

                    setTimeout(function() {
                        $alert.fadeOut();
                    }, 5000);
                }

                e.preventDefault();
                return false;
            }
        }, true);

        // ===========================================
        // ПАТЧИМ ФУНКЦИЮ SWITCHTAB ДЛЯ СОВМЕСТИМОСТИ
        // ===========================================
        if (window.App && window.App.switchTab) {
            var originalSwitchTab = window.App.switchTab;
            window.App.switchTab = function(tabName) {
                var result = originalSwitchTab.call(this, tabName);
                $(document).trigger('tabChanged', [tabName]);
                return result;
            };
        }
    });
})(jQuery);
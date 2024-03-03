linkUrl_userScript();

var linkUrl_tooltipElem;

function linkUrl_userScript() {

    addStyle();

    function addStyle() {
        const style = document.createElement("style");
        style.innerHTML = '' +
            '.linkUrl_tooltip { ' +
            '    position: fixed; ' +
            '    padding: 10px 20px; ' +
            '    border: 1px solid #b3c9ce; ' +
            '    border-radius: 4px; ' +
            '    text-align: center; ' +
            '    font: italic 14px/1.3 sans-serif; ' +
            '    color: #333; ' +
            '    background: #fff; ' +
            '    box-shadow: 3px 3px 3px rgba(0, 0, 0, .3); ' +
            '    overflow-wrap: break-word; ' +
            '    word-break: break-all;  ' +
            '} ';
        document.head.appendChild(style);
    }

    function setLinksCallbacks() {
        const links = document.querySelectorAll("a");
        for (var i = 0; i < links.length; i++) {
            setLinkCallbacks(links[i]);
        }
    }

    function setLinkCallbacks(linkElem) {
        // or onmouseover/click
        linkElem.oncontextmenu = function(e) {
             e.preventDefault();
             RE.clearSelection();

            onShowTooltip(linkElem.href, e);
        }

        linkElem.onmouseout = onRemoveTooltip;
    }

    function onShowTooltip(href, e) {
         const target = e.target;

         const tooltipElem = document.createElement("div");
         tooltipElem.className = "linkUrl_tooltip";

         const innerLinkElem = document.createElement("a");
         innerLinkElem.href = href;
         innerLinkElem.innerText = href;
         tooltipElem.appendChild(innerLinkElem);

         document.body.appendChild(tooltipElem);

         const coords = target.getBoundingClientRect();
         var left = coords.left + (target.offsetWidth - tooltipElem.offsetWidth) / 2;
         if (left < 0) left = 0;
         var top = coords.top - tooltipElem.offsetHeight - 5;
         if (top < 0) {
             top = coords.top + target.offsetHeight + 5;
         }
         var maxWidth = RE.editor.offsetWidth - 26;

         tooltipElem.style.left = left + 'px';
         tooltipElem.style.top = top + 'px';
         if (maxWidth > 0) {
             tooltipElem.style.maxWidth = maxWidth + 'px';
         }
         linkUrl_tooltipElem = tooltipElem;
    };

    const onRemoveTooltip = function(e) {

    // FIXME
        const elem = e.relatedTarget;
        const divElem = (elem.nodeName.toLowerCase() == "div") ? elem : elem.parentNode;

        if (linkUrl_tooltipElem && linkUrl_tooltipElem !== divElem) {
            linkUrl_tooltipElem.remove();
            linkUrl_tooltipElem = null;
        } else if (divElem) {
            divElem.onmouseout = function(e) {
                divElem.remove();
            }
        }
    };

    function removeTooltips() {
        const elements = document.getElementsByClassName("linkUrl_tooltip");
        while (elements.length > 0) {
            elements[0].parentNode.removeChild(elements[0]);
        }
    }

    RE.addOnBeforeSaveHtmlContentEventListener(function () {
        removeTooltips();
    });

    RE.addOnAfterLoadHtmlContentEventListener(function () {
        setLinksCallbacks();
    });
}
package com.myfootballcareer.game;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Android 1.0.4 tester launcher.
 * Keeps the scroll hotfix and guarantees a neutral new-career name field.
 * Existing career/save names are never modified.
 */
public class NeutralCareerActivity extends ScrollActivity {
    private WebView gameWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameWebView = findWebView(getWindow().getDecorView());
        if (gameWebView != null) {
            gameWebView.postDelayed(this::installNeutralNameGuard, 350);
            gameWebView.postDelayed(this::installNeutralNameGuard, 1200);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameWebView == null) gameWebView = findWebView(getWindow().getDecorView());
        if (gameWebView != null) gameWebView.postDelayed(this::installNeutralNameGuard, 250);
    }

    private WebView findWebView(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void installNeutralNameGuard() {
        if (gameWebView == null) return;
        gameWebView.evaluateJavascript(
            "(function(){" +
            "if(window.__mfcNeutralCareerNameV104){window.__mfcNeutralCareerNameV104.scan();return;}" +
            "function isCreator(){var t=(document.body&&document.body.innerText)||'';return t.indexOf('Twoja historia zaczyna się od marzenia')>=0&&t.indexOf('Nowa kariera zostanie zapisana')>=0;}" +
            "function findNameInput(){var labels=document.querySelectorAll('label,.label,[class*=\\\"label\\\"],h1,h2,h3,h4,div,span');" +
            "for(var i=0;i<labels.length;i++){var e=labels[i];if((e.textContent||'').trim()!=='Imię i nazwisko')continue;" +
            "var box=e.closest('div,section,form')||e.parentElement;if(box){var inp=box.querySelector('input[type=\\\"text\\\"],input:not([type])');if(inp)return inp;}" +
            "var n=e.nextElementSibling;while(n){var q=n.matches&&n.matches('input')?n:n.querySelector&&n.querySelector('input[type=\\\"text\\\"],input:not([type])');if(q)return q;n=n.nextElementSibling;}}" +
            "var inputs=document.querySelectorAll('input[type=\\\"text\\\"],input:not([type])');return inputs.length?inputs[0]:null;}" +
            "function scan(){if(!isCreator())return;var input=findNameInput();if(!input)return;input.placeholder='Wpisz imię i nazwisko';" +
            "var v=(input.value||'').trim();if(v==='Dawid Sadowski'||v==='Dawid'){input.value='';input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));}" +
            "input.dataset.mfcNeutralized='1';}" +
            "var api={scan:scan};window.__mfcNeutralCareerNameV104=api;" +
            "new MutationObserver(function(){scan();}).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['value']});" +
            "scan();" +
            "})();", null);
    }
}

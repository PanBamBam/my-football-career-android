package com.myfootballcareer.game;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Android 1.0.5 tester launcher.
 * Keeps the scroll/name guards and fixes the in-career save-slot flow:
 * active slot = save now, empty slot = create a save copy, new career only outside an active career.
 */
public class NeutralCareerActivity extends ScrollActivity {
    private WebView gameWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameWebView = findWebView(getWindow().getDecorView());
        if (gameWebView != null) {
            gameWebView.postDelayed(this::installUiGuards, 350);
            gameWebView.postDelayed(this::installUiGuards, 1200);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameWebView == null) gameWebView = findWebView(getWindow().getDecorView());
        if (gameWebView != null) gameWebView.postDelayed(this::installUiGuards, 250);
    }

    private void installUiGuards() {
        installNeutralNameGuard();
        installSaveSlotGuard();
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

    private void installSaveSlotGuard() {
        if (gameWebView == null) return;
        gameWebView.evaluateJavascript(
            "(function(){" +
            "if(window.__mfcSaveSlotFixV105){window.__mfcSaveSlotFixV105.scan();return;}" +
            "function inGame(){var g=document.getElementById('gameScreen');return !!(g&&g.classList.contains('active'));}" +
            "function slotNo(v){v=Number(v);return Number.isInteger(v)&&v>=1&&v<=3?v:1;}" +
            "function activeSlotNo(){try{return slotNo(localStorage.getItem('orlikLegendActiveSlot')||1);}catch(e){return 1;}}" +
            "function key(s){return 'orlikLegendCareerSlot_'+slotNo(s);}" +
            "function stored(s){try{return localStorage.getItem(key(s));}catch(e){return null;}}" +
            "function text(el,v){if(el&&el.textContent!==v)el.textContent=v;}" +
            "function scan(){var modal=document.getElementById('saveSlotsModal'),grid=document.getElementById('saveSlotsGrid');if(!modal||!grid)return;" +
            "var live=inGame(),title=document.getElementById('saveSlotsTitle'),head=modal.querySelector('.save-slots-head'),desc=head&&head.querySelector('p.muted');" +
            "if(live){text(title,'Zapis gry');text(desc,'Postęp zapisuje się automatycznie w aktywnym slocie. Możesz też wymusić zapis lub utworzyć kopię w wolnym slocie.');}" +
            "else{text(title,'Wybierz karierę');text(desc,'Trzy niezależne sloty. Gra tworzy automatyczne kopie bezpieczeństwa przy ważnych zmianach postępu.');}" +
            "var cards=grid.querySelectorAll('.save-slot');for(var i=0;i<cards.length;i++){var card=cards[i],any=card.querySelector('[data-slot]');if(!any)continue;var s=slotNo(any.dataset.slot),occupied=!!stored(s);" +
            "var play=card.querySelector('[data-slot-action=\\\"play\\\"],[data-slot-action=\\\"mfc-save-now\\\"]');var fresh=card.querySelector('[data-slot-action=\\\"new\\\"],[data-slot-action=\\\"mfc-copy\\\"]');var del=card.querySelector('[data-slot-action=\\\"delete\\\"]');" +
            "if(!live){if(play&&play.dataset.slotAction==='mfc-save-now'){play.dataset.slotAction='play';text(play,'Graj');}if(fresh&&fresh.dataset.slotAction==='mfc-copy'){fresh.dataset.slotAction='new';fresh.style.display='';text(fresh,'+ Nowa kariera');}if(del)del.style.display='';continue;}" +
            "if(occupied&&s===activeSlotNo()){if(play){play.dataset.slotAction='mfc-save-now';text(play,'💾 Zapisz teraz');}if(fresh)fresh.style.display='none';if(del)del.style.display='none';}" +
            "else if(!occupied){var h=card.querySelector('h4'),p=card.querySelector('p');text(h,'Pusty slot');text(p,'Utwórz kopię bieżącej kariery bez przerywania gry.');if(fresh){fresh.style.display='';fresh.dataset.slotAction='mfc-copy';text(fresh,'📋 Utwórz kopię');}}" +
            "else{if(play)text(play,'Wczytaj');if(fresh)fresh.style.display='none';if(del)del.style.display='none';}}}" +
            "function saveNow(){if(!inGame())return;try{if(typeof v950AbsorbPending==='function')v950AbsorbPending();if(typeof v950DoSave==='function')v950DoSave({critical:true});else if(typeof saveState==='function')saveState({critical:true});if(typeof renderSaveSlots==='function')renderSaveSlots();scan();if(typeof showToast==='function')showToast('Zapisano karierę w slocie '+activeSlotNo()+'.');}catch(e){console.warn('MFC manual save failed',e);if(typeof showToast==='function')showToast('Nie udało się zapisać kariery.');}}" +
            "function copyTo(s){if(!inGame())return;s=slotNo(s);if(stored(s)){if(typeof showToast==='function')showToast('Ten slot jest już zajęty.');return;}try{if(typeof v950FlushSave==='function')v950FlushSave('slot-copy');else if(typeof saveState==='function')saveState({critical:true});var source=stored(activeSlotNo());if(!source)throw new Error('Brak aktywnego zapisu.');var data=JSON.parse(source);data.career=data.career||{};data.career.saveMeta=data.career.saveMeta||{};data.career.saveMeta.slot=s;data.career.saveMeta.updatedAt=new Date().toISOString();data.career.saveMeta.revision=(Number(data.career.saveMeta.revision)||0)+1;localStorage.setItem(key(s),JSON.stringify(data));if(typeof renderSaveSlots==='function')renderSaveSlots();scan();if(typeof showToast==='function')showToast('Utworzono kopię kariery w slocie '+s+'.');}catch(e){console.warn('MFC save copy failed',e);if(typeof showToast==='function')showToast('Nie udało się utworzyć kopii zapisu.');}}" +
            "document.addEventListener('click',function(e){var b=e.target&&e.target.closest?e.target.closest('[data-slot-action=\\\"mfc-save-now\\\"],[data-slot-action=\\\"mfc-copy\\\"]'):null;if(!b)return;e.preventDefault();e.stopPropagation();if(b.dataset.slotAction==='mfc-save-now')saveNow();else copyTo(b.dataset.slot);},true);" +
            "new MutationObserver(function(){scan();}).observe(document.documentElement,{subtree:true,childList:true});window.__mfcSaveSlotFixV105={scan:scan,saveNow:saveNow,copyTo:copyTo};scan();" +
            "})();", null);
    }
}

'use strict';
const $=s=>document.querySelector(s), cats=['tops','bottoms','dresses','outerwear','shoes','bags','accessories','hats'], seasons=['All season','Spring/Summer','Autumn/Winter','Resort'], occasions=['Everyday','Workwear','Evening','Formal','Travel'];
let state={items:[],looks:[]}, view='home', category='all', selection={}, visible=[], busy=false, previewUrl;
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const title=s=>s.charAt(0).toUpperCase()+s.slice(1), slot=c=>({tops:'base',dresses:'base',bottoms:'bottom',outerwear:'layer',shoes:'footwear',bags:'bag',accessories:'detail',hats:'headwear'}[c]);
function toast(message){$('#toast').textContent=message;$('#toast').hidden=false;clearTimeout(toast.timer);toast.timer=setTimeout(()=>$('#toast').hidden=true,4200);}
let csrfToken, registering=false, studioCategory='tops', renderVersion=0;
const reducedMotion=()=>matchMedia('(prefers-reduced-motion: reduce)').matches;
function openUpload(){$('#upload-error').textContent='';$('#upload-dialog').showModal();}
async function getCsrf(){const res=await fetch('/api/auth/csrf');if(!res.ok)throw Error('Cannot connect. Please try again.');csrfToken=await res.json();}
function showLogin(){
 renderVersion++;
 state={items:[],looks:[]};selection={};visible=[];
 for(const id of ['home','wardrobe','looks','mixer','outfit-preview','selected-summary'])$('#'+id).replaceChildren();
 for(const dialog of document.querySelectorAll('dialog[open]'))dialog.close();
 $('#upload-form').reset();$('#save-form').reset();$('#auth-password').value='';
 if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}$('#preview').removeAttribute('src');$('#preview').hidden=true;
 $('#account-email').textContent='';$('#closet-app').hidden=true;$('#auth-panel').hidden=false;
}
async function api(url,options={}){
 const method=(options.method||'GET').toUpperCase();
 if(!['GET','HEAD'].includes(method)){if(!csrfToken)await getCsrf();options.headers={...options.headers,[csrfToken.headerName]:csrfToken.token};}
 const response=await fetch(url,options);
 if(!response.ok){
  if(response.status===401&&!url.endsWith('/login'))showLogin();
  if(response.status===403){csrfToken=null;}
  let message='Something went wrong. Please try again.';try{message=(await response.json()).message||message;}catch{}
  const error=Error(message);error.status=response.status;throw error;
 }
 return response.status===204?null:response.json();
}
async function openCloset(){
 const user=await api('/api/auth/me');
 $('#account-email').textContent=user.email;
 category='all';view='home';selection={};$('#season').value='all';$('#occasion').value='all';$('#search').value='';
 document.querySelectorAll('[data-category]').forEach(b=>(b.classList.toggle('active',b.dataset.category==='all'),b.setAttribute('aria-pressed',b.dataset.category==='all')));
 await refresh();await setView('home');$('#loading').hidden=true;
 $('#auth-panel').hidden=true;$('#closet-app').hidden=false;
}
$('#auth-toggle').onclick=()=>{
 registering=!registering;$('#auth-error').textContent='';$('#auth-password').value='';
 $('#auth-copy').textContent=registering?'Create an account for your own wardrobe and saved looks.':'Sign in to your personal collection.';
 $('#auth-submit').textContent=registering?'Create account ↗':'Sign in ↗';
 $('#auth-toggle').textContent=registering?'Already have an account? Sign in':'New here? Create an account';
 $('#password-hint').hidden=!registering;$('#auth-password').minLength=registering?12:1;
 $('#auth-password').autocomplete=registering?'new-password':'current-password';
};
$('#auth-form').onsubmit=async e=>{
 e.preventDefault();$('#auth-submit').disabled=true;$('#auth-toggle').disabled=true;$('#auth-error').textContent='';
 const email=$('#auth-email').value.trim(),password=$('#auth-password').value;
 try{
  await getCsrf();
  if(registering){await api('/api/auth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password})});$('#auth-toggle').onclick();toast('Account created. Opening your wardrobe…');}
  await api('/api/auth/login',{method:'POST',body:new URLSearchParams({email,password})});
  $('#auth-password').value='';await getCsrf();await openCloset();
 }catch(err){$('#auth-error').textContent=err.message;}finally{$('#auth-submit').disabled=false;$('#auth-toggle').disabled=false;}
};
$('#logout').onclick=()=>guarded(async()=>{await api('/api/auth/logout',{method:'POST'});showLogin();await getCsrf();$('#auth-email').focus();});
function query(){return new URLSearchParams({category,season:$('#season').value,occasion:$('#occasion').value});}
async function refresh(){state=await api('/api/state');for(const s in selection)if(!state.items.some(i=>i.id===selection[s]))delete selection[s];await render();}
async function render(){
 const version=++renderVersion;$('#count').textContent=state.items.length+' pieces · '+state.looks.length+' saved '+(state.looks.length===1?'look':'looks');
 $('#main-content').setAttribute('aria-busy','true');$('#loading').hidden=false;
 try{const params=query();params.set('category',view==='wardrobe'?category:'all');const items=await api('/api/items?'+params);
 if(version!==renderVersion)return;visible=items;
 if(view==='home')renderHome();if(view==='wardrobe')renderWardrobe();if(view==='studio')renderStudio();if(view==='looks')renderLooks();
 }finally{if(version===renderVersion){$('#loading').hidden=true;$('#main-content').setAttribute('aria-busy','false');}}
}
function image(i){return `<img src="${esc(i.image)}" alt="${esc(i.name)}" loading="lazy">`;}
function empty(heading,text,action=''){return `<div class="empty"><h2>${heading}</h2><p>${text}</p>${action}</div>`;}

function composition(items,interactive=false){
 if(!items.length)return empty('Your next look starts here.','Choose a piece below, or let Shuffle find a combination.');
 const stacked=items.every(i=>['base','bottom','footwear'].includes(slot(i.category))),detailsOnly=items.every(i=>!['base','bottom','footwear'].includes(slot(i.category)));
 return '<div class="composition '+(stacked?'stacked ':'')+(detailsOnly?'details-only ':'')+(items.some(i=>i.category==='dresses')?'has-dress ':'')+(items.length===1?'single':'')+'">'+items.map(i=>'<div class="composition-piece position-'+slot(i.category)+'">'+(interactive?'<button class="piece-remove" data-clear="'+slot(i.category)+'" aria-label="Remove '+esc(i.name)+' from look">×</button>':'')+image(i)+'</div>').join('')+'</div>';
}
function renderHome(){
 const latest=state.looks[0],featured=latest?latest.items.map(id=>state.items.find(i=>i.id===id)).filter(Boolean):state.items.slice(0,3);
 $('#home').innerHTML=state.items.length?'<div class="home-feature"><div class="feature-art">'+composition(featured)+'</div><div class="feature-copy"><p class="eyebrow">'+(latest?'FROM YOUR LOOKBOOK':'FROM YOUR COLLECTION')+'</p><h2>'+ (latest?esc(latest.name):'Possibilities, <em>already yours.</em>')+'</h2><p class="hint">'+(latest?'A combination worth coming back to. Make it yours all over again.':'Start with a piece you love. See what comes together.')+'</p><button class="dark" '+(latest?'data-open-look="'+latest.id+'"':'data-go-studio')+'>Style an outfit ↗</button></div></div><div class="section-heading home-section"><div><p class="eyebrow">THE EVERYDAY EDIT</p><h2>A few familiar <em>favourites.</em></h2></div><button class="ghost" data-view="wardrobe">View wardrobe ↗</button></div><div class="home-pieces">'+state.items.slice(0,4).map(i=>'<button data-select="'+i.id+'" class="home-piece">'+image(i)+'<span>'+esc(i.name)+'</span></button>').join('')+'</div>':empty('A little space. All yours.','Add your first piece and begin discovering the possibilities in your wardrobe.','<button class="dark" data-add>Add your first piece ↗</button>');
 $('#home').insertAdjacentHTML('beforeend','<div class="home-links"><button data-go-studio><span>01 / THE STUDIO</span>Create a look ↗</button><button data-add><span>02 / SOMETHING NEW</span>Add a piece ↗</button><button data-view="looks"><span>03 / YOUR LOOKBOOK</span>Saved looks ↗</button></div>');
}
function renderWardrobe(){
 const needle=$('#search').value.trim().toLowerCase(),items=visible.filter(i=>i.name.toLowerCase().includes(needle));
 $('#wardrobe').innerHTML=items.length?items.map((i,n)=>'<article class="card"><span class="number">'+String(n+1).padStart(2,'0')+' / '+esc(i.category)+'</span><button class="image-button" data-select="'+i.id+'" aria-label="Style '+esc(i.name)+'">'+image(i)+'</button><h3>'+esc(i.name)+'</h3><p>'+esc(i.season)+' · '+esc(i.occasion)+'</p><div class="card-bottom"><button class="ghost" data-select="'+i.id+'">Style ↗</button><button class="remove" data-delete-item="'+i.id+'" aria-label="Remove '+esc(i.name)+'">Remove</button></div></article>').join(''):state.items.length?empty('Nothing in this edit.','Try another category, search, season, or occasion.','<button data-reset>Clear filters</button>'):empty('Room for something new.','Your personal wardrobe begins with one piece.','<button class="dark" data-add>Add your first piece ↗</button>');
}
function renderStudio(){
 const categoryScroll=$('#studio-categories').scrollLeft;
 const chosen=Object.values(selection).map(id=>state.items.find(i=>i.id===id)).filter(Boolean),pool=visible.filter(i=>i.category===studioCategory);
 $('#outfit-preview').innerHTML=composition(chosen,true);
 $('#studio-status').textContent=chosen.length?chosen.length+' '+(chosen.length===1?'piece':'pieces')+' selected'+(chosen.some(i=>i.category==='dresses')?' · A dress replaces your top and bottom.':' · Tap × on a piece to remove it.'):'Your canvas is open. Every position is optional.';
 $('#studio-categories').innerHTML=cats.map(c=>'<button data-studio-category="'+c+'" aria-pressed="'+(c===studioCategory)+'" class="'+(c===studioCategory?'active':'')+'">'+title(c)+'</button>').join('');
 $('#mixer').innerHTML='<div class="section-heading picker-heading"><span>'+title(studioCategory)+' · '+pool.length+' available</span><span class="hint">Swipe to browse →</span></div><div class="garment-rail">'+(pool.length?pool.map(i=>'<button class="garment-choice '+(selection[slot(i.category)]===i.id?'selected':'')+'" data-pick="'+i.id+'" aria-pressed="'+(selection[slot(i.category)]===i.id)+'">'+image(i)+'<span>'+esc(i.name)+'</span><small>'+(selection[slot(i.category)]===i.id?'✓ Selected':'Choose piece')+'</small></button>').join(''):'<div class="picker-empty"><p>No '+esc(studioCategory)+' in this edit.</p><button data-add>Add a piece</button><button class="ghost" data-reset>Clear filters</button></div>')+'</div>';
 $('#studio-categories').scrollLeft=categoryScroll;
 $('#selected-summary').textContent=chosen.length+' '+(chosen.length===1?'piece':'pieces')+' in this look';
 $('#save-form button[type=submit]').disabled=$('#save-open').disabled=!chosen.length;$('#clear').disabled=!chosen.length;$('#shuffle').disabled=!visible.length;
}
function renderLooks(){
 const looks=state.looks.filter(l=>($('#season').value==='all'||l.season===$('#season').value)&&($('#occasion').value==='all'||l.occasion===$('#occasion').value));
 $('#looks').innerHTML=looks.length?looks.map(l=>'<article class="look-card"><button class="look-cover" data-open-look="'+l.id+'" aria-label="Open '+esc(l.name)+' in studio">'+composition(l.items.map(id=>state.items.find(i=>i.id===id)).filter(Boolean))+'</button><div class="look-details"><p class="eyebrow">'+esc(l.season)+' / '+esc(l.occasion)+'</p><h2>'+esc(l.name)+'</h2><p class="hint">'+new Date(l.created).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'})+' · '+l.items.length+' '+(l.items.length===1?'piece':'pieces')+'</p><div class="card-bottom"><button class="ghost" data-open-look="'+l.id+'">Style again ↗</button><button class="remove" data-delete-look="'+l.id+'" aria-label="Delete '+esc(l.name)+'">Delete look</button></div></div></article>').join(''):state.looks.length?empty('A different kind of day.','No saved looks match these filters.','<button data-reset>Clear filters</button>'):empty('Good looks are worth keeping.','Create a combination in the studio, then save it here for another day.','<button class="dark" data-go-studio>Create your first look ↗</button>');
}
async function setView(next){
 if(!['home','wardrobe','studio','looks'].includes(next))return;
 view=next;for(const v of ['home','wardrobe','studio','looks'])$('#'+v).hidden=v!==view;
 document.querySelectorAll('[data-view]').forEach(b=>{b.classList.toggle('active',b.dataset.view===view);if(b.dataset.view===view)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});
 $('#toolbar').hidden=view==='home';$('#categories').hidden=view!=='wardrobe';$('#search-label').hidden=view!=='wardrobe';
 const copy={home:['YOUR PERSONAL WARDROBE','Welcome back.<br><em>Make it yours.</em>','A new day. A familiar wardrobe.<br> A different way to wear it.'],wardrobe:['01 / THE COLLECTION','A wardrobe,<br><em>considered.</em>','The pieces you love.<br> The possibilities within them.'],studio:['02 / THE OUTFIT STUDIO','A little mix.<br><em>A lot of you.</em>','Choose a piece. Find your rhythm.<br> Make a look for your day.'],looks:['03 / THE LOOKBOOK','Good looks,<br><em>remembered.</em>','Your favourite combinations.<br> Ready when you are.']}[view];
 $('#chapter').textContent=copy[0];$('#heading').innerHTML=copy[1];$('#description').innerHTML=copy[2];document.body.dataset.view=view;
 await render();
}
function choose(id){const i=state.items.find(p=>p.id===id);if(!i)return;selection[slot(i.category)]=id;if(i.category==='dresses')delete selection.bottom;if(i.category==='bottoms'&&state.items.find(p=>p.id===selection.base)?.category==='dresses')delete selection.base;}
async function guarded(action){if(busy)return;busy=true;try{await action();}catch(e){toast(e.message||'Cannot connect. Check that ATELIER is running.');}finally{busy=false;}}
function confirmDelete(message,action){$('#confirm-message').textContent=message;$('#confirm-dialog').showModal();$('#confirm-delete').onclick=()=>guarded(async()=>{$('#confirm-delete').disabled=true;try{await action();$('#confirm-dialog').close();await refresh();toast('Removed from your collection.');}finally{$('#confirm-delete').disabled=false;}});}
document.addEventListener('click',e=>{const b=e.target.closest('button');if(!b||(b.closest('form')&&b.type==='submit')||b.id||b.classList.contains('close'))return;guarded(async()=>{
 if(b.dataset.view){await setView(b.dataset.view);window.scrollTo({top:0,behavior:reducedMotion()?'instant':'smooth'});}
 if(b.dataset.close)$('#'+b.dataset.close).close();
 if(b.hasAttribute('data-add'))openUpload();
 if(b.dataset.studioCategory){studioCategory=b.dataset.studioCategory;renderStudio();$('#studio-categories [data-studio-category="'+studioCategory+'"]').focus({preventScroll:true});}
 if(b.dataset.pick){choose(b.dataset.pick);const left=$('.garment-rail').scrollLeft;renderStudio();$('.garment-rail').scrollLeft=left;$('#mixer [data-pick="'+b.dataset.pick+'"]').focus({preventScroll:true});}
 if(b.dataset.category){category=b.dataset.category;document.querySelectorAll('[data-category]').forEach(x=>(x.classList.toggle('active',x===b),x.setAttribute('aria-pressed',x===b)));await render();}
 if(b.dataset.select){choose(b.dataset.select);await setView('studio');window.scrollTo({top:0,behavior:reducedMotion()?'instant':'smooth'});}
 if(b.dataset.clear){delete selection[b.dataset.clear];renderStudio();}
 if(b.dataset.deleteItem){const i=state.items.find(i=>i.id===b.dataset.deleteItem);confirmDelete(`Remove “${i.name}”? It will also be removed from saved looks. Looks with no remaining pieces are deleted.`,()=>api('/api/items/'+i.id,{method:'DELETE'}));}
 if(b.dataset.deleteLook){const l=state.looks.find(l=>l.id===b.dataset.deleteLook);confirmDelete(`Delete “${l.name}” from your lookbook? Your wardrobe pieces stay available.`,()=>api('/api/looks/'+l.id,{method:'DELETE'}));}
 if(b.dataset.openLook){selection={};const l=state.looks.find(l=>l.id===b.dataset.openLook);l.items.forEach(choose);$('#save-form [name=name]').value=l.name;$('#save-form [name=season]').value=l.season;$('#save-form [name=occasion]').value=l.occasion;await setView('studio');}
 if(b.hasAttribute('data-reset')){category='all';$('#season').value='all';$('#occasion').value='all';$('#search').value='';document.querySelectorAll('[data-category]').forEach(x=>(x.classList.toggle('active',x.dataset.category==='all'),x.setAttribute('aria-pressed',x.dataset.category==='all')));await render();}
 if(b.hasAttribute('data-go-studio'))await setView('studio');
});});
$('#add').onclick=openUpload;
$('#account-open').onclick=()=>$('#account-dialog').showModal();
$('#save-open').onclick=()=>{$('#save-error').textContent='';$('#save-dialog').showModal();};$('.close').onclick=()=>$('#upload-dialog').close();$('#cancel-delete').onclick=()=>$('#confirm-dialog').close();
$('#season').onchange=$('#occasion').onchange=()=>guarded(render);$('#search').oninput=renderWardrobe;
$('#clear').onclick=()=>{selection={};renderStudio();};
$('#shuffle').onclick=()=>guarded(async()=>{const items=await api('/api/shuffle?'+query());selection={};items.forEach(i=>choose(i.id));renderStudio();toast(items.length?'A fresh combination, just for you.':'No pieces match. Try another season or occasion.');});
$('#save-form').onsubmit=e=>{e.preventDefault();guarded(async()=>{const button=e.target.querySelector('[type=submit]');button.disabled=true;button.textContent='Saving…';$('#save-error').textContent='';try{const form=new FormData(e.target);await api('/api/looks',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:form.get('name'),season:form.get('season'),occasion:form.get('occasion'),items:Object.values(selection)})});$('#save-dialog').close();await refresh();toast('A good look, remembered. Saved to your lookbook.');}catch(err){$('#save-error').textContent=err.message;}finally{button.disabled=false;button.textContent='Save to lookbook ↗';}});};
$('#upload-form [name=image]').onchange=e=>{const file=e.target.files[0];if(previewUrl)URL.revokeObjectURL(previewUrl);$('#preview').hidden=true;if(file){if(file.size>8*1024*1024){$('#upload-error').textContent='Choose an image under 8 MB.';e.target.value='';return;}previewUrl=URL.createObjectURL(file);$('#preview').src=previewUrl;$('#preview').hidden=false;}};
$('#upload-form').onsubmit=e=>{e.preventDefault();guarded(async()=>{const button=e.target.querySelector('[type=submit]');button.disabled=true;button.textContent='Adding your piece…';$('#upload-error').textContent='';try{await api('/api/items',{method:'POST',body:new FormData(e.target)});e.target.reset();$('#preview').hidden=true;if(previewUrl)URL.revokeObjectURL(previewUrl);$('#upload-dialog').close();await refresh();toast('Your new piece is in the wardrobe.');}catch(err){$('#upload-error').textContent=err.message;}finally{button.disabled=false;button.textContent='Add to wardrobe ↗';}});};
function options(values){return values.map(v=>`<option>${esc(v)}</option>`).join('');}
document.querySelectorAll('.season-options').forEach(s=>s.innerHTML=options(seasons));document.querySelectorAll('.occasion-options').forEach(s=>s.innerHTML=options(occasions));$('#season').insertAdjacentHTML('beforeend',options(seasons));$('#occasion').insertAdjacentHTML('beforeend',options(occasions));$('#upload-category').innerHTML=cats.map(c=>`<option value="${c}">${title(c)}</option>`).join('');$('#categories').innerHTML=['all',...cats].map(c=>`<button data-category="${c}" aria-pressed="${c==='all'}" class="${c==='all'?'active':''}">${c==='all'?'All pieces':title(c)}</button>`).join('');
guarded(async()=>{try{await getCsrf();await openCloset();}catch(e){if(e.status!==401)$('#auth-error').textContent=e.message;}});

function fitSheets(){const viewport=window.visualViewport;document.documentElement.style.setProperty('--sheet-height',Math.floor(viewport?viewport.height:window.innerHeight)+'px');}
window.visualViewport?.addEventListener('resize',fitSheets);fitSheets();

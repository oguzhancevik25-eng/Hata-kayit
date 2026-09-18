const CLOUD_URL_PC='https://etdhqgrxhicqjkfsoxue.supabase.co';
const CLOUD_KEY_PC='sb_publishable_ECvS4jam6myE7gAeSF7a0Q_viLhocWB';
const CLOUD_CODE_KEY_PC='operatorTakipPC_syncCode_v16';
let cloudStatusPC='';
let cloudBusyPC=false;

function getCloudCodePC(){return (localStorage.getItem(CLOUD_CODE_KEY_PC)||'').trim()}
function setCloudCodePC(code){localStorage.setItem(CLOUD_CODE_KEY_PC,(code||'').trim().toUpperCase())}

async function rpcPC(name,body){
  const res=await fetch(CLOUD_URL_PC+'/rest/v1/rpc/'+name,{
    method:'POST',
    headers:{'Content-Type':'application/json','apikey':CLOUD_KEY_PC},
    body:JSON.stringify(body)
  });
  const text=await res.text();
  if(!res.ok){
    let msg=text;
    try{msg=JSON.parse(text).message||text}catch(e){}
    if(String(msg).toLowerCase().includes('invalid sync code'))msg='Senkron kodu yanlış';
    throw new Error(msg||('Bulut bağlantı hatası '+res.status));
  }
  return text?JSON.parse(text):null;
}

function cloudStateFromDbPC(){
  return {
    schemaVersion:1,
    operators:db.operators,
    records:db.records.map(function(r){
      return {
        id:r.id,timestamp:r.timestamp,operatorSicil:r.operatorSicil,type:r.type,
        defect:r.defect||'',amount:Number(r.amount||0),machine:r.machine||'',
        part:r.part||'',note:r.note||'',photoData:r.photoData||r.photo||''
      };
    }),
    refs:db.refs||{},
    machines:db.machines||defaults.machines,
    parts:db.parts||defaults.parts,
    scoring:Object.assign({},defaultScoring,db.scoring||{})
  };
}

function applyCloudStatePC(state){
  if(!state||typeof state!=='object')return;
  cloudApplyingPC=true;
  try{
    db.operators=Array.isArray(state.operators)&&state.operators.length?state.operators:db.operators;
    db.records=Array.isArray(state.records)?state.records.map(function(r){
      return {
        id:Number(r.id||Date.now()),timestamp:Number(r.timestamp||Date.now()),
        operatorSicil:String(r.operatorSicil||''),type:String(r.type||''),
        defect:String(r.defect||''),amount:Number(r.amount||0),
        machine:String(r.machine||''),part:String(r.part||''),note:String(r.note||''),
        photo:r.photoData||r.photo||''
      };
    }):db.records;
    db.refs=state.refs&&typeof state.refs==='object'?state.refs:db.refs;
    db.machines=Array.isArray(state.machines)&&state.machines.length?state.machines:db.machines;
    db.parts=Array.isArray(state.parts)&&state.parts.length?state.parts:db.parts;
    db.scoring=Object.assign({},defaultScoring,state.scoring||db.scoring||{});
    save();
  } finally {
    cloudApplyingPC=false;
    cloudDirtyPC=false;
  }
  render();
}

async function syncCloudPC(forcePush){
  const code=getCloudCodePC();
  if(!code){cloudStatusPC='Senkron kodu girilmedi';if(current==='sync')sync();return}
  if(cloudBusyPC)return;
  cloudBusyPC=true;
  if(current==='sync')sync();
  try{
    if(forcePush||cloudDirtyPC){
      await rpcPC('operator_sync_push',{p_code:code,p_state:cloudStateFromDbPC()});
      cloudDirtyPC=false;
      cloudStatusPC='✓ Bilgisayardaki değişiklikler buluta gönderildi';
    }else{
      const outer=await rpcPC('operator_sync_pull',{p_code:code});
      if(!outer||outer.state==null){
        await rpcPC('operator_sync_push',{p_code:code,p_state:cloudStateFromDbPC()});
        cloudDirtyPC=false;
        cloudStatusPC='✓ İlk bilgisayar verisi buluta yüklendi';
      }else{
        cloudStatusPC='✓ Telefon ve PC verileri eşitlendi';
        applyCloudStatePC(outer.state);
      }
    }
  }catch(e){
    cloudStatusPC=e&&e.message?e.message:'Senkron hatası';
  }finally{
    cloudBusyPC=false;
    if(current==='sync')sync();
  }
}

function sync(){
  const code=getCloudCodePC();
  page.innerHTML=
    '<div class="top"><div><div class="title">Telefon + PC Senkron</div><div class="sub">Aynı kodu telefonda ve bilgisayarda bir kez kaydet. Veriler yaklaşık 15 saniyede bir eşitlenir.</div></div></div>'+
    '<div class="grid two"><div class="card"><div class="sectionTitle">Senkron Kodu</div>'+
    '<div class="field"><label>Kod</label><input id="cloudCodePC" value="'+esc(code)+'" placeholder="OT-...."></div>'+
    '<button class="btn primary" style="margin-top:10px" onclick="saveCloudCodePC()">KODU KAYDET</button></div>'+
    '<div class="card"><div class="sectionTitle">Bulut Durumu</div>'+
    '<div class="'+(String(cloudStatusPC).startsWith('✓')?'success':'hint')+'">'+esc(cloudStatusPC||'Henüz senkron yapılmadı.')+'</div>'+
    '<button class="btn green" style="margin-top:10px" '+(cloudBusyPC||!code?'disabled':'')+' onclick="syncCloudPC(false)">'+(cloudBusyPC?'SENKRON YAPILIYOR...':'ŞİMDİ SENKRONİZE ET')+'</button></div></div>'+
    '<div class="card" style="margin-top:16px"><div class="sectionTitle">Ortak Veriler</div>'+
    '<div class="sub">Operatörler, kayıtlar, hata fotoğrafları, makineler, parçalar ve puanlama ayarları ortak bulutta tutulur. İnternet yokken PC çevrimdışı çalışmaya devam eder.</div></div>';
}

function saveCloudCodePC(){
  const el=document.getElementById('cloudCodePC');
  const code=(el?el.value:'').trim().toUpperCase();
  if(!code){alert('Senkron kodunu girin');return}
  setCloudCodePC(code);
  cloudStatusPC='Kod kaydedildi, bağlantı kuruluyor...';
  cloudDirtyPC=false;
  sync();
  syncCloudPC(false);
}

setInterval(function(){
  if(getCloudCodePC()&&!cloudBusyPC)syncCloudPC(false);
},15000);

setTimeout(function(){
  if(getCloudCodePC())syncCloudPC(false);
},1800);

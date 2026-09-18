let managementSectionPC='Ekip';
let selectedMonthPC='';
let trendSicilPC='';

function periodMonthsPC(){
  let out=[],d=new Date(2026,7,1);
  for(let i=0;i<12;i++){
    let key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
    let label=d.toLocaleDateString('tr-TR',{month:'long',year:'numeric'});
    label=label.charAt(0).toUpperCase()+label.slice(1);
    out.push({key,label}); d=new Date(d.getFullYear(),d.getMonth()+1,1);
  }
  return out;
}

function monthRowsPC(op,key){
  return db.records.filter(function(r){
    if(r.operatorSicil!==op.sicil)return false;
    let d=new Date(r.timestamp);
    let k=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
    return k===key;
  });
}

function monthly(){
  let ops=activeOps();
  if(!ops.length){page.innerHTML='<div class="card warn">Aktif personel yok.</div>';return;}
  let ms=periodMonthsPC();
  if(!selectedMonthPC){
    let n=new Date(),k=n.getFullYear()+'-'+String(n.getMonth()+1).padStart(2,'0');
    selectedMonthPC=ms.some(function(x){return x.key===k})?k:ms[0].key;
  }
  if(!trendSicilPC||!ops.some(function(o){return o.sicil===trendSicilPC}))trendSicilPC=ops[0].sicil;
  let month=ms.find(function(x){return x.key===selectedMonthPC})||ms[0];
  let scores=ops.map(function(o){return {op:o,s:scoreRows(monthRowsPC(o,selectedMonthPC),1)}});
  let recCount=scores.reduce(function(a,x){return a+x.s.rows},0);
  let escaped=scores.reduce(function(a,x){return a+x.s.escaped},0);
  let caught=scores.reduce(function(a,x){return a+x.s.caught},0);
  let trendOp=ops.find(function(o){return o.sicil===trendSicilPC})||ops[0];
  let trend=ms.map(function(m){return {m:m,s:scoreRows(monthRowsPC(trendOp,m.key),1)}});
  let chart=trend.map(function(x){
    let v=x.s.rows?Math.round(x.s.total):0;
    return '<div style="display:grid;grid-template-columns:105px 1fr 45px;gap:10px;align-items:center;margin:7px 0"><span class="sub">'+esc(x.m.label.split(' ')[0])+'</span><div class="bar"><i style="width:'+v+'%"></i></div><b>'+(x.s.rows?fmt(x.s.total):'—')+'</b></div>';
  }).join('');
  let cards=scores.map(function(x){
    let s=x.s,o=x.op;
    return '<div class="card"><div style="display:flex;justify-content:space-between;gap:15px;align-items:flex-start"><div><div class="sectionTitle">'+esc(o.name)+'</div><div class="sub">Sicil '+esc(o.sicil)+' • '+s.rows+' kayıt</div></div><div style="text-align:right"><div class="score" style="font-size:30px">'+(s.rows?fmt(s.total):'—')+'</div><span class="pill '+(s.grade==='A'?'p-green':s.grade==='B'?'p-blue':s.grade==='C'?'p-yellow':'p-red')+'">'+s.grade+'</span></div></div><div class="tablewrap"><table class="table"><tr><td>KY</td><td><b>'+fmt(s.KY)+'</b></td><td>Kaizen</td><td><b>'+fmt(s.kaizen)+'</b></td><td>Mesai</td><td><b>'+fmt(s.mesai)+' s</b></td></tr><tr><td>Yakalanan</td><td><b>'+fmt(s.caught)+'</b></td><td>Kaçan</td><td><b>'+fmt(s.escaped)+'</b></td><td>Devamsızlık</td><td><b>'+fmt(s.devam)+' g</b></td></tr><tr><td>Yıllık İzin</td><td><b>'+fmt(s.annualLeave)+' g</b></td><td>Günlük İzin</td><td><b>'+fmt(s.dailyLeave)+' g</b></td><td>Rapor</td><td><b>'+fmt(s.rapor)+' g</b></td></tr></table></div></div>';
  }).join('');
  page.innerHTML='<div class="top"><div><div class="title">Aylık Operatör Analizi</div><div class="sub">Her ay operatörlerin yaptığı işleri ve puanlarını ayrı ayrı gör.</div></div></div>' +
    '<div class="grid kpis"><div class="card kpi"><div class="n">'+recCount+'</div><div class="l">Toplam Kayıt</div></div><div class="card kpi"><div class="n">'+fmt(escaped)+'</div><div class="l">Kaçan Hata</div></div><div class="card kpi"><div class="n">'+fmt(caught)+'</div><div class="l">Yakalanan Hata</div></div></div>' +
    '<div class="grid two" style="margin-top:16px"><div class="card"><div class="sectionTitle">Ay Seç</div><div class="field"><select onchange="selectedMonthPC=this.value;monthly()">'+ms.map(function(m){return '<option value="'+m.key+'" '+(m.key===selectedMonthPC?'selected':'')+'>'+esc(m.label)+'</option>'}).join('')+'</select></div></div>' +
    '<div class="card"><div class="sectionTitle">12 Aylık Puan Grafiği</div><div class="field"><label>Operatör</label><select onchange="trendSicilPC=this.value;monthly()">'+ops.map(function(o){return '<option value="'+esc(o.sicil)+'" '+(o.sicil===trendSicilPC?'selected':'')+'>'+esc(o.sicil+' - '+o.name)+'</option>'}).join('')+'</select></div>'+chart+'</div></div>' +
    '<div class="sectionTitle" style="margin-top:20px">'+esc(month.label)+' • Operatörler</div><div class="grid two">'+cards+'</div>';
}

function management(){
  let tabs=['Ekip','Makineler','Parça / Kalıp'];
  let tabHtml=tabs.map(function(t){return '<button class="btn '+(managementSectionPC===t?'primary':'ghost')+'" onclick="managementSectionPC=\''+t.replace("'","\\'")+'\';management()">'+esc(t)+'</button>'}).join('');
  let body='';
  if(managementSectionPC==='Ekip'){
    body='<div class="grid two"><div class="card"><div class="sectionTitle">Yeni Operatör</div><div class="formgrid"><div class="field span2"><label>Sicil No</label><input id="newSicilPC"></div><div class="field span2"><label>Ad Soyad</label><input id="newNamePC"></div><div class="span4"><button class="btn green" onclick="addOperatorPC()">OPERATÖR EKLE</button></div></div></div><div class="card"><div class="sectionTitle">Durum</div><div class="sub">Aktif: '+db.operators.filter(function(x){return x.active}).length+' • Pasif: '+db.operators.filter(function(x){return !x.active}).length+'</div></div></div><div class="card" style="margin-top:16px"><div class="tablewrap"><table class="table"><thead><tr><th>Sicil</th><th>Ad Soyad</th><th>Durum</th><th>İşlem</th></tr></thead><tbody>'+db.operators.map(function(o){return '<tr><td>'+esc(o.sicil)+'</td><td>'+esc(o.name)+'</td><td><span class="pill '+(o.active?'p-green':'p-red')+'">'+(o.active?'AKTİF':'PASİF')+'</span></td><td><button class="btn '+(o.active?'danger':'green')+'" onclick="toggleOperatorPC(\''+esc(o.sicil)+'\')">'+(o.active?'ÇIKAR':'GERİ AL')+'</button></td></tr>'}).join('')+'</tbody></table></div></div>';
  } else if(managementSectionPC==='Makineler'){
    body='<div class="grid two"><div class="card"><div class="sectionTitle">Yeni Makine</div><div class="field"><label>Makine Adı</label><input id="newMachinePC" placeholder="Örn. 1800T"></div><button class="btn green" style="margin-top:10px" onclick="addMachinePC()">MAKİNE EKLE</button></div><div class="card"><div class="sectionTitle">Kayıtlı Makine</div><div class="score" style="font-size:36px">'+db.machines.length+'</div></div></div><div class="card" style="margin-top:16px"><div class="tablewrap"><table class="table"><thead><tr><th>Makine</th><th>Bağlı Parça</th><th>İşlem</th></tr></thead><tbody>'+db.machines.map(function(m){let linked=db.parts.filter(function(p){return p.machine===m}).length;return '<tr><td><b>'+esc(m)+'</b></td><td>'+linked+'</td><td><button class="btn danger" onclick="removeMachinePC(\''+String(m).replace(/'/g,"\\'")+'\')">ÇIKAR</button></td></tr>'}).join('')+'</tbody></table></div></div>';
  } else {
    let machineOptions=db.machines.map(function(m){return '<option>'+esc(m)+'</option>'}).join('');
    body='<div class="card"><div class="sectionTitle">Yeni Parça / Kalıp</div><div class="formgrid"><div class="field span2"><label>Parça / Kalıp Adı</label><input id="newPartPC"></div><div class="field span2"><label>Makine</label><select id="newPartMachinePC">'+machineOptions+'</select></div><div class="span4"><button class="btn green" onclick="addPartPC()">PARÇA EKLE</button></div></div></div><div class="card" style="margin-top:16px"><div class="tablewrap"><table class="table"><thead><tr><th>Parça / Kalıp</th><th>Makine</th><th>İşlem</th></tr></thead><tbody>'+db.parts.map(function(p,i){return '<tr><td>'+esc(p.name)+'</td><td>'+esc(p.machine)+'</td><td><button class="btn danger" onclick="removePartPC('+i+')">ÇIKAR</button></td></tr>'}).join('')+'</tbody></table></div></div>';
  }
  page.innerHTML='<div class="top"><div><div class="title">Yönetim</div><div class="sub">Operatör, makine ve parça listelerini uygulama içinden yönet.</div></div></div><div class="toolbar" style="margin-bottom:16px">'+tabHtml+'</div>'+body;
}

function addOperatorPC(){
  let s=document.getElementById('newSicilPC').value.trim(),n=document.getElementById('newNamePC').value.trim().toLocaleUpperCase('tr-TR');
  if(!s||!n){alert('Sicil ve ad soyad girin');return}
  if(db.operators.some(function(x){return x.sicil===s})){alert('Bu sicil zaten kayıtlı');return}
  db.operators.push({sicil:s,name:n,active:true});save();management();
}
function toggleOperatorPC(s){let o=db.operators.find(function(x){return x.sicil===s});if(o){o.active=!o.active;save();management()}}
function addMachinePC(){
  let n=document.getElementById('newMachinePC').value.trim().toLocaleUpperCase('tr-TR');
  if(!n){alert('Makine adı girin');return}
  if(db.machines.some(function(x){return x.toLowerCase()===n.toLowerCase()})){alert('Bu makine zaten kayıtlı');return}
  db.machines.push(n);save();management();
}
function removeMachinePC(m){
  if(db.machines.length<=1){alert('En az 1 makine kalmalı');return}
  let linked=db.parts.filter(function(p){return p.machine===m}).length;
  if(linked){alert(m+' makinesine '+linked+' parça bağlı. Önce bağlı parçaları çıkarın.');return}
  if(confirm(m+' makinesi listeden çıkarılsın mı?')){db.machines=db.machines.filter(function(x){return x!==m});save();management()}
}
function addPartPC(){
  let n=document.getElementById('newPartPC').value.trim(),m=document.getElementById('newPartMachinePC').value;
  if(!n){alert('Parça / kalıp adı girin');return}
  if(db.parts.some(function(x){return x.name.toLowerCase()===n.toLowerCase()})){alert('Bu parça zaten kayıtlı');return}
  db.parts.push({name:n,machine:m});save();management();
}
function removePartPC(i){if(confirm('Bu parça listeden çıkarılsın mı? Eski kayıtlar silinmez.')){db.parts.splice(i,1);save();management()}}

function scoring(){
  let s=db.scoring;
  let fields=[
    ['qualityMax','Kalite maksimum puan'],['kyMax','KY maksimum puan'],['kaizenMax','Kaizen maksimum puan'],['attendanceMax','Devam maksimum puan'],['overtimeMax','Mesai maksimum puan'],
    ['qualityStart','Kalite başlangıç puanı'],['escapedPenalty','Kaçan hata cezası × katsayı'],['caughtBonus','Yakalanan hata bonusu × katsayı'],['caughtBonusCap','Yakalanan hata bonus tavanı'],
    ['kyMonthlyTarget','Aylık KY hedefi'],['kaizenMonthlyTarget','Aylık Kaizen hedefi'],['overtimeMonthlyTarget','Aylık mesai hedefi (saat)'],['absencePenaltyPerDay','Devamsızlık cezası / gün'],
    ['gradeA','A sınıfı alt sınır'],['gradeB','B sınıfı alt sınır'],['gradeC','C sınıfı alt sınır']
  ];
  let input=function(k,l){return '<div class="field"><label>'+esc(l)+'</label><input type="number" step="0.1" id="sc_'+k+'" value="'+esc(s[k])+'"></div>'};
  page.innerHTML='<div class="top"><div><div class="title">Puanlama Ayarları</div><div class="sub">Telefon v1.5 ile aynı mantık. Kaydedince tüm eski kayıtlar yeni puana göre yeniden hesaplanır.</div></div></div>' +
    '<div class="card"><div class="sectionTitle">Puan Dağılımı</div><div class="formgrid">'+fields.slice(0,5).map(function(x){return input(x[0],x[1])}).join('')+'</div><div class="hint" style="margin-top:10px">Toplam ağırlığı istediğin gibi değiştirebilirsin; sonuç 100 üzerinden normalize edilir.</div></div>' +
    '<div class="grid two" style="margin-top:16px"><div class="card"><div class="sectionTitle">Kalite Ayarları</div><div class="formgrid">'+fields.slice(5,9).map(function(x){return input(x[0],x[1])}).join('')+'</div></div>' +
    '<div class="card"><div class="sectionTitle">Hedef ve Devamsızlık</div><div class="formgrid">'+fields.slice(9,13).map(function(x){return input(x[0],x[1])}).join('')+'</div><div class="sub" style="margin-top:10px">Yıllık izin, günlük izin ve rapor varsayılan olarak puan düşürmez.</div></div></div>' +
    '<div class="card" style="margin-top:16px"><div class="sectionTitle">Sınıf Sınırları</div><div class="formgrid">'+fields.slice(13).map(function(x){return input(x[0],x[1])}).join('')+'</div><div class="toolbar" style="margin-top:14px"><button class="btn primary" onclick="saveScoringPC()">PUANLAMAYI KAYDET</button><button class="btn ghost" onclick="resetScoringPC()">VARSAYILANA DÖN</button></div></div>';
}

function saveScoringPC(){
  let keys=Object.keys(defaultScoring),s={};
  for(let i=0;i<keys.length;i++){let k=keys[i],el=document.getElementById('sc_'+k),v=Number(el.value);if(!Number.isFinite(v)){alert('Tüm alanlara geçerli sayı girin');return}s[k]=v}
  if([s.qualityMax,s.kyMax,s.kaizenMax,s.attendanceMax,s.overtimeMax].some(function(v){return v<0})||s.qualityMax+s.kyMax+s.kaizenMax+s.attendanceMax+s.overtimeMax<=0){alert('Puan ağırlıkları geçersiz');return}
  if(s.qualityStart<0||s.qualityStart>s.qualityMax){alert('Kalite başlangıç puanı, kalite maksimum puanını geçemez');return}
  if(s.kyMonthlyTarget<=0||s.kaizenMonthlyTarget<=0||s.overtimeMonthlyTarget<=0){alert('Aylık hedefler 0 dan büyük olmalı');return}
  if(!(s.gradeA>s.gradeB&&s.gradeB>s.gradeC&&s.gradeA<=100&&s.gradeC>=0)){alert('Sınıf sınırları A > B > C ve 0-100 aralığında olmalı');return}
  db.scoring=s;save();alert('Puanlama kaydedildi. Tüm puanlar yeniden hesaplandı.');scoring();
}
function resetScoringPC(){if(confirm('Varsayılan puanlamaya dönülsün mü?')){db.scoring=Object.assign({},defaultScoring);save();scoring()}}
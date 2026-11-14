'use strict';
AR.DICTIONARIES.DICT_4X4_50 = { ...AR.DICTIONARIES.ARUCO_4X4_1000, codeList: AR.DICTIONARIES.ARUCO_4X4_1000.codeList.slice(0, 50), tau: null };
const detector = new AR.Detector({dictionaryName:'DICT_4X4_50'});
const $ = id => document.getElementById(id);
const video=$('video'), overlay=$('overlay'), ctx=overlay.getContext('2d'), camera=document.querySelector('.camera');
const capture=document.createElement('canvas'), captureCtx=capture.getContext('2d',{willReadFrequently:true});
let stream=null, facing='environment', animation=0, recording=false, samples=[], lastSample=0, latest=null, startedAt=0, loopToken=0;
const fields=['x','y','z','distance','rx','ry','rz'];
function clearReadings(){ for(const id of fields) $(id).textContent='—'; $('status').textContent='SIN SEÑAL'; $('status').classList.remove('found'); latest=null; }
function stopStream(){ cancelAnimationFrame(animation); loopToken++; if(stream) stream.getTracks().forEach(t=>t.stop()); stream=null; video.srcObject=null; camera.classList.remove('active'); clearReadings(); }
async function startCamera(next=facing){
  if(!navigator.mediaDevices?.getUserMedia){ $('message').textContent='La cámara necesita una conexión HTTPS o localhost y un navegador compatible.'; return; }
  stopStream(); $('start').disabled=true; $('switch').disabled=true;
  try {
    stream=await navigator.mediaDevices.getUserMedia({audio:false,video:{facingMode:{ideal:next},width:{ideal:1280},height:{ideal:720}}});
    facing=next; video.srcObject=stream; await video.play(); camera.classList.add('active');
    $('camera-label').textContent=facing==='environment'?'TRASERA':'FRONTAL';
    $('start').textContent='■   Detener cámara'; $('switch').disabled=false; $('record').disabled=false;
    $('message').textContent='Apunta al marcador ArUco 4×4. El cálculo usa un campo visual aproximado.';
    const token=++loopToken; animation=requestAnimationFrame(t=>tick(t,token));
  } catch(e){ stopStream(); $('record').disabled=true; $('start').textContent='▶   Iniciar cámara'; $('message').textContent='No se pudo abrir la cámara. Revisa los permisos del navegador y vuelve a intentarlo.'; }
  finally { $('start').disabled=false; }
}
function tick(time,token){
 if(token!==loopToken||!stream)return;
 try { if(video.readyState>=2){
  const w=640,h=Math.round(w*video.videoHeight/video.videoWidth); if(h>0){
   if(capture.width!==w||capture.height!==h){ capture.width=w;capture.height=h;overlay.width=w;overlay.height=h; }
   captureCtx.drawImage(video,0,0,w,h); const markers=detector.detect(captureCtx.getImageData(0,0,w,h)); ctx.clearRect(0,0,w,h);
   if(markers.length){ const m=markers[0];ctx.strokeStyle='#c5f279';ctx.lineWidth=3;ctx.beginPath();m.corners.forEach((p,i)=>i?ctx.lineTo(p.x,p.y):ctx.moveTo(p.x,p.y));ctx.closePath();ctx.stroke();
    const corners=m.corners.map(p=>({x:p.x-w/2,y:h/2-p.y})); const focal=w/(2*Math.tan(Math.PI/6)); const pose=new POS.Posit(5,focal).pose(corners);
    if(pose && pose.bestRotation && pose.bestTranslation){const t=pose.bestTranslation,r=pose.bestRotation, sy=Math.hypot(r[0][0],r[1][0]);
     const values={x:t[0],y:t[1],z:t[2],distance:Math.hypot(...t),rx:Math.atan2(r[2][1],r[2][2])*180/Math.PI,ry:Math.atan2(-r[2][0],sy)*180/Math.PI,rz:Math.atan2(r[1][0],r[0][0])*180/Math.PI};
     if(Object.values(values).every(Number.isFinite)){latest={id:m.id,...values};for(const id of fields)$(id).textContent=values[id].toFixed(id.startsWith('r')?0:1)+(id.startsWith('r')?'°':'');$('status').textContent='ID '+m.id+' DETECTADO';$('status').classList.add('found');}else clearReadings();
    }else clearReadings();
   }else clearReadings();
   if(recording && latest && time-lastSample>=100){samples.push({time:(performance.now()-startedAt)/1000,...latest});lastSample=time;$('record-info').textContent=samples.length+' mediciones guardadas.';}
  }
 }}catch(e){clearReadings();$('message').textContent='No se pudo analizar la imagen. Prueba reiniciar la cámara.';}
 animation=requestAnimationFrame(t=>tick(t,token));
}
$('start').onclick=()=>{if(stream){stopStream();recording=false;$('record').textContent='●   Iniciar registro';$('record').disabled=true;$('download').disabled=!samples.length;$('record-info').textContent=samples.length+' mediciones guardadas.';$('switch').disabled=true;$('start').textContent='▶   Iniciar cámara';$('message').textContent='Cámara detenida.';}else startCamera();};
$('switch').onclick=()=>startCamera(facing==='environment'?'user':'environment');
$('record').onclick=()=>{recording=!recording;if(recording){samples=[];startedAt=performance.now();lastSample=0;$('record-info').textContent='Grabando mediciones…';}else $('record-info').textContent=samples.length+' mediciones guardadas.';$('record').textContent=recording?'■   Detener registro':'●   Iniciar registro';$('download').disabled=recording||!samples.length;};
$('download').onclick=()=>{const columns=['time','id',...fields],csv=['tiempo_s,id,x_cm,y_cm,z_cm,distancia_cm,rotacion_x_grados,rotacion_y_grados,rotacion_z_grados',...samples.map(s=>columns.map(k=>s[k]).join(','))].join('\n');const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));a.download='registro-marcador.csv';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);};
window.addEventListener('pagehide',stopStream);

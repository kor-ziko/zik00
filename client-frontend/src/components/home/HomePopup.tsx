import X from 'lucide-react/dist/esm/icons/x.js';
import { useEffect, useRef, useState } from 'react';
import type { MouseEvent as ReactMouseEvent, PointerEvent as ReactPointerEvent } from 'react';
import { useHomepageContent } from '../../homepage/HomepageContentContext';
import { managedHref } from '../../homepage/managedLink';

function HomePopup(){
  const popups=useHomepageContent('POPUP');
  const [closedIds,setClosedIds]=useState<Set<number>>(()=>new Set());
  const [position,setPosition]=useState<{x:number;y:number}|null>(null);
  const popupRef=useRef<HTMLElement|null>(null);
  const dragRef=useRef<{pointerX:number;pointerY:number;x:number;y:number}|null>(null);
  const movedRef=useRef(false);
  const preview=new URLSearchParams(window.location.search).has('previewPopup');
  const popup=popups.find(item=>!closedIds.has(item.id)&&(preview||!hiddenToday(item.id)));
  useEffect(()=>{if(!popup)return;const close=(e:KeyboardEvent)=>{if(e.key==='Escape')setClosedIds(current=>new Set(current).add(popup.id))};window.addEventListener('keydown',close);return()=>window.removeEventListener('keydown',close)},[popup]);
  useEffect(()=>{
    const move=(event:PointerEvent)=>{
      const start=dragRef.current;
      const popupElement=popupRef.current;
      if(!start||!popupElement)return;
      const deltaX=event.clientX-start.pointerX;
      const deltaY=event.clientY-start.pointerY;
      if(Math.hypot(deltaX,deltaY)<5)return;
      movedRef.current=true;
      const box=popupElement.getBoundingClientRect();
      setPosition({
        x:clamp(start.x+deltaX,8,window.innerWidth-box.width-8),
        y:clamp(start.y+deltaY,8,window.innerHeight-box.height-8),
      });
    };
    const end=()=>{dragRef.current=null};
    window.addEventListener('pointermove',move);
    window.addEventListener('pointerup',end);
    window.addEventListener('pointercancel',end);
    return()=>{
      window.removeEventListener('pointermove',move);
      window.removeEventListener('pointerup',end);
      window.removeEventListener('pointercancel',end);
    };
  },[]);
  if(!popup)return null;
  const close=()=>setClosedIds(current=>new Set(current).add(popup.id));
  const hideToday=()=>{window.localStorage.setItem(storageKey(popup.id),todayKey());close()};
  const image=popup.imageUrl&&<img className="home-popup-image" src={popup.imageUrl} alt=""/>;
  const startDrag=(event:ReactPointerEvent<HTMLElement>)=>{if((event.target as HTMLElement).closest('button,.home-popup-footer'))return;const box=popupRef.current?.getBoundingClientRect();if(!box)return;movedRef.current=false;dragRef.current={pointerX:event.clientX,pointerY:event.clientY,x:box.left,y:box.top};event.preventDefault()};
  const followImageLink=(event:ReactMouseEvent<HTMLAnchorElement>)=>{if(!movedRef.current)return;event.preventDefault();event.stopPropagation();movedRef.current=false};
  return <div className="home-popup-layer" style={position?{left:position.x,top:position.y}:undefined}><article ref={popupRef} onPointerDown={startDrag} className={popup.imageUrl?'home-popup has-image is-draggable':'home-popup is-draggable'} role="dialog" aria-modal="false" aria-labelledby={`popup-${popup.id}`}>
    <button type="button" className="home-popup-close" onClick={close} aria-label="팝업 닫기"><X size={19}/></button>
    {popup.linkUrl&&image?<a className="home-popup-image-link" href={managedHref(popup.linkUrl)} onClick={followImageLink} draggable="false" aria-label={`${popup.title} 페이지로 이동`}>{image}</a>:image}
    <div className={popup.imageUrl?'home-popup-copy compact':'home-popup-copy'}><small>{popup.subtitle}</small><h2 id={`popup-${popup.id}`}>{popup.title}</h2>{popup.content&&<p>{popup.content}</p>}</div>
    <footer className="home-popup-footer"><button type="button" onClick={hideToday}>오늘 하루 안보기</button><button type="button" onClick={close}>닫기</button></footer>
  </article></div>;
}

function storageKey(id:number){return `zik00-popup-hidden-${id}`}
function todayKey(){const now=new Date();return `${now.getFullYear()}-${now.getMonth()+1}-${now.getDate()}`}
function hiddenToday(id:number){return window.localStorage.getItem(storageKey(id))===todayKey()}
function clamp(value:number,min:number,max:number){return Math.min(Math.max(value,min),Math.max(min,max))}
export default HomePopup;

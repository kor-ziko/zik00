import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export type HomepageContent = {
  id:number; contentType:string; title:string; subtitle:string|null; content:string|null;
  imageUrl:string|null; linkUrl:string|null; linkLabel:string|null; applicationType:string|null; displayOrder:number;
  active:boolean; startsAt:string|null; endsAt:string|null;
};

type ContextValue={items:HomepageContent[];byType:(type:string)=>HomepageContent[]};
const HomepageContentContext=createContext<ContextValue>({items:[],byType:()=>[]});

export function HomepageContentProvider({children}:{children:ReactNode}){
  const [items,setItems]=useState<HomepageContent[]>([]);
  useEffect(()=>{let active=true;fetch('/api/homepage-content').then(r=>r.ok?r.json():Promise.reject()).then(data=>{if(active)setItems(data)}).catch(()=>{});return()=>{active=false}},[]);
  const value=useMemo(()=>({items,byType:(type:string)=>items.filter(item=>item.contentType===type)}),[items]);
  return <HomepageContentContext.Provider value={value}>{children}</HomepageContentContext.Provider>;
}

export function useHomepageContent(type:string){return useContext(HomepageContentContext).byType(type)}

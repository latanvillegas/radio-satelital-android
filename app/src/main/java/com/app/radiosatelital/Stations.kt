// stations.js v9.5
// =======================
// DATA FRECUENCIAS v8.5 (Tu Lista Personalizada)
// =======================

const defaultStations = [
  // =======================================================
  // ====== ESTACIONES CONSOLIDADAS DEL NUEVO ARCHIVO ======
  // =======================================================
  { name: "La Mega", country: "Venezuela", region: "", url: "https://eu1.lhdserver.es:9007/stream" },
  { name: "Disney FM", country: "Perú", region: "", url: "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC" },
  { name: "Acqua", country: "Argentina", region: "Mar del Plata", url: "https://sonic-us.streaming-chile.com:7006/" },
  { name: "Acqua 100.1", country: "Argentina", region: "Villa Gesell", url: "https://strcdn.klm99.com:10983/acquapinamar" },
  { name: "Arceri", country: "Costa Rica", region: "Aserrín", url: "https://stream-178.zeno.fm/rmnr2cphyxhvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJybW5yMmNwaHl4aHZ2IiwiaG9zdCI6InN0cmVhbS0xNzguemVuby5mbSIsInJ0dGwiOjUsImp0aSI6ImRYMW41R2s4U08yYWx2V0JKcHRDWEEiLCJpYXQiOjE3NjU0MTkxOTEsImV4cCI6MTc2NTQxOTI1MX0.KWxdZmmrrZmvw7-yDyEuMHt6eBzZ2z_OfEElTQVFrPQ" },
  { name: "Actitud", country: "México", region: "San Felipe", url: "https://radioenhd.com:8088/" },
  { name: "Actitud 100.9 FM", country: "Guatemala", region: "Ciudad de Guatemala", url: "https://ss.redradios.net:8002/stream" },
  { name: "Activa (Buenos Aires)", country: "Argentina", region: "Buenos Aires", url: "https://edge01.radiohdvivo.com/fmra1033" },
  { name: "Activa (Colombia)", country: "Colombia", region: "Tuquerres", url: "https://stream-177.zeno.fm/vkb12zqmgzzuv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJ2a2IxMnpxbWd6enV2IiwiaG9zdCI6InN0cmVhbS0xNzcuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InVSNGNtcWRqUks2Q1dYb29xY2c5ckEiLCJpYXQiOjE3NjU0MTk2MDEsImV4cCI6MTc2NTQxOTY2MX0.mzmgEUJ2fr9JQ35IKW3sIeU7Ilioh400N-YjigAkiqM" },
  { name: "Activa (Salina Cruz)", country: "México", region: "Salina Cruz", url: "https://sp3.servidorrprivado.com/6617/" },
  { name: "Activa (Puerto Rico)", country: "Puerto Rico", region: "San Juan", url: "https://cast3.asurahosting.com/proxy/univers1/stream" },
  { name: "Activa 100.7", country: "Argentina", region: "News", url: "https://sh4.radioonlinehd.com:8533/stream" },
  { name: "Activa 12340 AM", country: "EE.UU", region: "Nashville", url: "https://ice66.securenetsystems.net/WNVL" },
  { name: "Activa 88.7", country: "Argentina", region: "Pocito", url: "https://streaming.radiosenlinea.com.ar/9088/stream" },
  { name: "Activa 92.5 CL", country: "Chile", region: "Chile", url: "https://stream-154.zeno.fm/pvs6hqz3crtvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJwdnM2aHF6M2NydHZ2IiwiaG9zdCI6InN0cmVhbS0xNTQuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InpMR0Q1SEJRUWctazNNa3hNN2xVSXciLCJpYXQiOjE3NjU0MjA3NjcsImV4cCI6MTc2NTQyMDgyN30.R4EmPlaA0KfJ41x2S7efsqu98v_gkSg_7t9xV_Qcwvs" },
  { name: "Activa 95.1 FM Católica", country: "Guatemala", region: "Guatemala", url: "https://sh4.radioonlinehd.com:8557/stream" },
  { name: "Activa 99.7 FM", country: "Ecuador", region: "Santo Domingo de Los Colorado", url: "https://stream-142.zeno.fm/6n7fp9t2pceuv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI2bjdmcDl0MnBjZXV2IiwiaG9zdCI6InN0cmVhbS0xNDIuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ill2OUlFS2N0Um9pZFFaWDRvclJaQkEiLCJpYXQiOjE3NjU0MjA5NDMsImV4cCI6MTc2NTQyMTAwM30.ZOf-uJfL4P364hvHh9raQmkUFQKFLrMpakK8EeSkXPo" },
  { name: "Activa El Salvador", country: "El Salvador", region: "San Miguel", url: "https://stream-179.zeno.fm/ar1f9iuyvhgtv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJhcjFmOWl1eXZoZ3R2IiwiaG9zdCI6InN0cmVhbS0xNzkuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InUzZHdHWGF4U3hpY01Rc3BQeWVvUmciLCJpYXQiOjE3NjU0MjEwMzgsImV4cCI6MTc2NTQyMTA5OH0.vcaNAVo91j9YcFO6MOW9XX2DMblnYGQ_yRXvm3yMhI0" },
  { name: "Activa FM (Chile)", country: "Chile", region: "Santiago Providencia", url: "https://27433.live.streamtheworld.com/ACTIVA.mp3" },
  { name: "Activa FM 93.5", country: "Argentina", region: "Los Cóndores", url: "https://streaming5.locucionar.com/proxy/activafm?mp=/stream" },
  { name: "Activa FM (Bolivia)", country: "Bolivia", region: "La Paz", url: "https://stream-179.zeno.fm/007z22fpx38uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiIwMDd6MjJmcHgzOHV2IiwiaG9zdCI6InN0cmVhbS0xNzkuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6IlBVYnFobWdOUlVHTU80M04zaDBXVWciLCJpYXQiOjE3NjU0MjE0MzUsImV4cCI6MTc2NTQyMTQ5NX0.j7G6_lvz-Dtb5lsSWIYiARLMAtIN5iy7WWKKjLuFnao" },
  { name: "Activa Jaén", country: "Perú", region: "Jaén, Cajamarca", url: "https://sp.onliveperu.com/8108/stream" },
  { name: "Activa La Paz", country: "Bolivia", region: "La Paz", url: "https://cloudstream2032.conectarhosting.com/9856/stream" },
  { name: "Activa Táchira Punta FM", country: "Venezuela", region: "San Cristóbal", url: "https://stream-157.zeno.fm/53vezhmmad0uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI1M3ZlemhtbWFkMHV2IiwiaG9zdCI6InN0cmVhbS0xNTcuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ii1vUTExOGtSU1dlZG82cHVzZk1JOEEiLCJpYXQiOjE3NjU0MjE3MjEsImV4cCI6MTc2NTQyMTc4MX0.Xqb3C5FezGjrayWw84Zmpn9tkMV0AIz4gcy8X3V152M" },
  
  // =======================================================
  // ====== PERÚ (LIMA / NACIONAL) ======
  // =======================================================
  { name: "Radio Moda", country: "Perú", region: "Nacional", url: "https://25023.live.streamtheworld.com/CRP_MOD_SC" },
  { name: "Ritmo Romántica", country: "Perú", region: "Nacional", url: "https://25103.live.streamtheworld.com/CRP_RIT_SC" },
  { name: "Onda Cero", country: "Perú", region: "Nacional", url: "https://mdstrm.com/audio/6598b65ab398c90871aff8cc/icecast.audio" },
  { name: "La Zona", country: "Perú", region: "Nacional", url: "https://mdstrm.com/audio/5fada54116646e098d97e6a5/icecast.audio" },
  { name: "Radio Corazón", country: "Perú", region: "Nacional", url: "https://mdstrm.com/audio/5fada514fc16c006bd63370f/icecast.audio" },
  { name: "La Inolvidable", country: "Perú", region: "Nacional", url: "https://playerservices.streamtheworld.com/api/livestream-redirect/CRP_LI_SC" },
  { name: "Radio Mágica", country: "Perú", region: "Nacional", url: "https://26513.live.streamtheworld.com/MAG_AAC_SC" },
  { name: "Radiomar", country: "Perú", region: "Nacional", url: "https://24873.live.streamtheworld.com/CRP_MARAAC_SC" },
  { name: "RPP Noticias", country: "Perú", region: "Nacional", url: "https://mdstrm.com/audio/5fab3416b5f9ef165cfab6e9/icecast.audio" },
  { name: "Exitosa Noticias", country: "Perú", region: "Nacional", url: "https://neptuno-2-audio.mediaserver.digital/79525baf-b0f5-4013-a8bd-3c5c293c6561" },
  { name: "Radio PBO", country: "Perú", region: "Nacional", url: "https://stream.radiojar.com/2fse67zuv8hvv" },
  { name: "Radio Inca", country: "Perú", region: "Nacional", url: "https://stream.zeno.fm/b9x47pyk21zuv" },
  { name: "Radio ABN", country: "Perú", region: "Nacional", url: "https://jml-stream.com/radio/8000/radio.mp3" },
  { name: "Radio Abba Padre", country: "Perú", region: "Nacional", url: "https://stream-175.zeno.fm/6rrwumthg6quv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI2cnJ3dW10aGc2cXV2IiwiaG9zdCI6InN0cmVhbS0xNzUuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Im9XY2g3dmlTU0NHYlVGQ0QtZmNxUFEiLCJpYXQiOjE3NjQ3OTMwNDksImV4cCI6MTc2NDc5MzEwOX0.U3kdYbFm_XjuESzU_aSQ7owwkG9ScWV9h4fLn36I88U" },

  // =======================================================
  // ====== PERÚ (REGIONALES) ======
  // =======================================================
  { name: "Radio Turbo Mix", country: "Perú", region: "Regional", url: "https://serverssl.innovatestream.pe:8080/167.114.118.120:7624/stream" },
  { name: "Radio Fuego", country: "Perú", region: "Regional", url: "https://serverssl.innovatestream.pe:8080/sp.onliveperu.com:8128/" },
  { name: "Radio Andina", country: "Perú", region: "Regional", url: "https://serverssl.innovatestream.pe:8080/http://167.114.118.120:7058/;stream" },
  { name: "Radio Ilucan", country: "Perú", region: "Regional", url: "https://serverssl.innovatestream.pe:8080/167.114.118.120:7820/;stream" },
  { name: "Radio Santa Lucía", country: "Perú", region: "Regional", url: "https://sp.dattavolt.com/8014/stream" },
  { name: "Radio Pampa Yurac", country: "Perú", region: "Regional", url: "https://rr5200.globalhost1.com/8242/stream" },
  { name: "Radio Stereo TV", country: "Perú", region: "Regional", url: "https://sp.onliveperu.com:7048/stream" },
  { name: "Radio La Kuadra", country: "Perú", region: "Regional", url: "https://dattavolt.com/8046/stream" },
  { name: "Radio Frecuencia", country: "Perú", region: "Regional", url: "https://conectperu.com/8384/stream" },
  { name: "Onda Popular (Lima)", country: "Perú", region: "Regional", url: "https://envivo.top:8443/am" },
  { name: "Onda Popular (Juliaca)", country: "Perú", region: "Regional", url: "https://dattavolt.com/8278/stream" },
  { name: "Radio Nor Andina", country: "Perú", region: "Regional", url: "https://mediastreamm.com/8012/stream/1/" },
  { name: "Radio Bambamarca", country: "Perú", region: "Regional", url: "https://envivo.top:8443/lider" },
  { name: "Radio Continente", country: "Perú", region: "Regional", url: "https://sonic6.my-servers.org/10170/" },
  { name: "La Cheverísima", country: "Perú", region: "Regional", url: "https://sp.onliveperu.com:8114/stream" },
  { name: "Radio TV El Shaddai", country: "Perú", region: "Regional", url: "https://stream.zeno.fm/ppr5q4q3x1zuv" },
  { name: "Radio Inica Digital", country: "Perú", region: "Regional", url: "https://stream.zeno.fm/487vgx80yuhvv" },
  { name: "Radio Activa", country: "Perú", region: "Regional", url: "https://sp.onliveperu.com:8108/stream" }, 
  { name: "Radio Mía", country: "Perú", region: "Regional", url: "https://streaming.zonalatinaeirl.com:8020/radio" },
  { name: "Radio Patrón", country: "Perú", region: "Regional", url: "https://streaming.zonalatinaeirl.com:8010/radio" },
  { name: "Radio TV Sureña", country: "Perú", region: "Regional", url: "https://stream.zeno.fm/p7d5fpx4xnhvv" },
  { name: "Radio Enamorados", country: "Perú", region: "Regional", url: "https://stream.zeno.fm/gnybbqc1fnruv" },

  // =======================================================
  // ====== LATINOAMÉRICA / NORTEAMÉRICA ======
  // =======================================================
  { name: "Radio ABC (San Luis)", country: "México", region: "Norteamérica", url: "https://16643.live.streamtheworld.com/XHCZFM.mp3" },
  { name: "Radio ABC (Taxco)", country: "México", region: "Norteamérica", url: "https://streaming.servicioswebmx.com/8288/stream" },
  { name: "ABC 760", country: "México", region: "Norteamérica", url: "https://streamingcwsradio30.com/8292/stream" },
  { name: "ABC Radio Puebla", country: "México", region: "Norteamérica", url: "https://streaming.servicioswebmx.com/8264/stream" },
  { name: "Radio Acceso Total", country: "México", region: "Norteamérica", url: "https://us10a.serverse.com/proxy/acce8712?mp=/" },
  { name: "Ach Kuxlejal 100.3", country: "México", region: "Norteamérica", url: "https://stream-178.zeno.fm/md6tfkaaechvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJtZDZ0ZmthYWVjaHZ2IiwiaG9zdCI6InN0cmVhbS0xNzguemVuby5mbSIsInJ0dGwiOjUsImp0aSI6IkI4TUMzLXR3UTR1Q1VzbXY2M0gwUFEiLCJpYXQiOjE3NjQ3OTQ4MTksImV4cCI6MTc2NDc5NDg3OX0.xBJOxw_oGdW4sqNsL4n9WyUeK6CTvzAY8o5i5MjLe78" },
  
  { name: "ABC 94.7", country: "Argentina", region: "Sudamérica", url: "https://stream-176.zeno.fm/n03jc4xoy63tv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJuMDNqYzR4b3k2M3R2IiwiaG9zdCI6InN0cmVhbS0xNzYuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Im9aa3kyRHRvUXBDU3kwNUF2OGdPX3ciLCJpYXQiOjE3NjQ3OTM2MzksImV4cCI6MTc2NDc5MzY5OX0.clgYVIm4DHZtHwGTjXdSfYi0SjVgGWj8UkiZEBz3yg0" },
  { name: "Estéreo Abejorral", country: "Colombia", region: "Sudamérica", url: "https://icecasthd.net/proxy/abejorral/live" },
  { name: "Abriendo Surcos", country: "Colombia", region: "Sudamérica", url: "https://djp.sytes.net/public/abriendo_surcos" },
  { name: "Acacio de Chile", country: "Chile", region: "Sudamérica", url: "https://sonic.portalfoxmix.cl:7057/" },
  { name: "Acción FM", country: "Venezuela", region: "Sudamérica", url: "https://stream-intervalohost.com:7008/stream" },
  { name: "Aclo Chuquisaca", country: "Bolivia", region: "Sudamérica", url: "https://cloudstream2030.conectarhosting.com/8192/stream" },
  { name: "Aclo Tarija", country: "Bolivia", region: "Sudamérica", url: "https://cloudstream2030.conectarhosting.com/8242/stream" },
  
  { name: "Radio La Hondureña", country: "Honduras", region: "Centroamérica", url: "https://s2.mkservers.space/rih" },
  { name: "Abriendo Los Cielos", country: "Honduras", region: "Centroamérica", url: "https://stream-177.zeno.fm/a8uwe88svy8uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJhOHV3ZTg4c3Z5OHV2IiwiaG9zdCI6InN0cmVhbS0xNzcuemVuby4mbSIsInJ0dGwiOjUsImp0aSI6Ik9VWVRibTdpUVUtQjVhSHFOWUNyX1EiLCJpYXQiOjE3NjQ3OTQwOTQsImV4cCI6MTc2NDc5NDE1NH0.n3CeLd9U7rcF9A9NsPpzFGJJjuPsUoaf2EsUxGah04w" },
  { name: "Una Radio Viva Voz", country: "Nicaragua", region: "Centroamérica", url: "https://rr5100.globalhost1.com/8006/stream" },
  
  // =======================================================
  // ====== EEUU / EUROPA ======
  // =======================================================
  { name: "105.3 El Ritmo", country: "EE.UU", region: "Norteamérica", url: "https://n02b-e2.revma.ihrhls.com/zc3209/hls.m3u8?rj-ttl=5&rj-tok=AAABmuXcB-4Ad7qhABJqQGGBcg" },
  { name: "Acción Cristiana", country: "EE.UU", region: "Norteamérica", url: "https://panel.lifestreammedia.net:8162/stream" },

  { name: "RFI Internacional", country: "Francia", region: "Europa", url: "https://rfienespagnol64k.ice.infomaniak.ch/rfienespagnol-64.mp3" },
  { name: "RFI Español (96k)", country: "Francia", region: "Europa", url: "https://rfiespagnol96k.ice.infomaniak.ch/rfiespagnol-96k.mp3" },
  { name: "DW Español", country: "Alemania", region: "Europa", url: "https://dwstream6-lh.akamaihd.net/i/dwstream6_live@123544/master.m3u8" },
  
  { name: "RNE 5 (España)", country: "España", region: "Europa", url: "https://dispatcher.rndfnk.com/crtve/rne5/main/mp3/high?aggregator=tunein" },
  { name: "RNE Radio Clásica", country: "España", region: "Europa", url: "https://rnelivestream.rtve.es/rnerc/main/master.m3u8" },
  { name: "RNE Radio Nacional", country: "España", region: "Europa", url: "https://f141.rndfnk.com/star/crtve/rne1/nav/mp3/128/ct/stream.mp3?cid=01GENZSPVYG0R84NK9E1C77RSZ&sid=36LhA65FiO252hsvxBqzfqiI4HF&token=-FbGT-8Eif8zgFPSMX7ER3TPiwAZ4pI8BsNKr1HldC4&tvf=HsCHLkTgfRhmMTQxLnJuZGZuay5jb20" },
  { name: "Radio AFRONTAR", country: "España", region: "Europa", url: "https://vigo-copesedes-rrcast.flumotion.com/copesedes/vigo-low.mp3" },
  { name: "AB 95 FM", country: "España", region: "Europa", url: "https://stream-153.zeno.fm/szskq9dxs98uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJzenNrcTlkeHM5OHV2IiwiaG9zdCI6InN0cmVhbS0xNTMuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ims0M0xwaVpDVE1pRXExWVhvMEpjUmciLCJpYXQiOjE3NjQ3OTI4NjUsImV4cCI6MTc2NDc5MjkyNX0.dpzu-0oLrJ2nsOJU25J8ghjMS2O_2FSyXzntk4rD05A" },
  { name: "Radio Tele Taxi", country: "España", region: "Europa", url: "https://radiott-web.streaming-pro.com:6103/radiott.mp3" },
  { name: "Radio ES", country: "España", region: "Europa", url: "https://libertaddigital-radio-live1.flumotion.com/libertaddigital/ld-live1-low.mp3" },
  { name: "Cadena COPE", country: "España", region: "Europa", url: "https://net1-cope-rrcast.flumotion.com/cope/net1-low.mp3" }
];

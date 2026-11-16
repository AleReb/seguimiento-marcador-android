#!/usr/bin/env python3
"""Generate printable A4 sheets. Requires OpenCV contrib 4.10, Pillow, ReportLab."""
from pathlib import Path
import cv2
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4, letter
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader

root = Path(__file__).resolve().parents[1]
out = root / 'docs/calibracion'
out.mkdir(parents=True, exist_ok=True)
dictionary = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_4X4_50)
board = cv2.aruco.CharucoBoard((5, 7), 0.030, 0.015, dictionary)
image = board.generateImage((1500, 2100), marginSize=0, borderBits=1)
# Verify generated symbols before packaging the board at its physical dimensions.
detector = cv2.aruco.ArucoDetector(dictionary)
_, ids, _ = detector.detectMarkers(image)
assert ids is not None and len(ids) == 17, 'Invalid generated ChArUco board'

def page(path, title, subtitle, paper=A4):
    c = canvas.Canvas(str(path), pagesize=paper)
    center = paper[0]/2
    top = paper[1]
    c.setTitle(title)
    c.setFont('Helvetica-Bold', 14)
    c.drawCentredString(center, top-15*mm, title)
    c.setFont('Helvetica', 10)
    c.drawCentredString(center, top-23*mm, subtitle)
    return c

def footer(c):
    center=c._pagesize[0]/2
    c.setFont('Helvetica', 10)
    c.drawCentredString(center, 24*mm, 'Imprimir al 100 %. Desactivar ajustar a pagina.')
    c.line(center-50*mm, 16*mm, center+50*mm, 16*mm)
    for x in (center-50*mm, center+50*mm): c.line(x, 14*mm, x, 18*mm)
    c.setFont('Helvetica', 8)
    c.drawCentredString(center, 10*mm, 'Esta linea debe medir exactamente 100 mm con una regla.')

c = page(out/'hoja-charuco-a4.pdf', 'Calibracion de camara - ChArUco', '5 x 7 cuadrados de 30 mm | Marcadores de 15 mm | DICT_4X4_50')
c.drawImage(ImageReader(Image.fromarray(image)), 30*mm, 43.5*mm, width=150*mm, height=210*mm)
c.setFont('Helvetica', 9)
c.drawCentredString(105*mm, 35*mm, 'Tablero: 150 x 210 mm. Pegar sobre una superficie plana y rigida.')
footer(c); c.save()

marker = cv2.aruco.generateImageMarker(dictionary, 0, 500)
# The detector requires a white margin around a standalone marker.
import numpy as np
padded = np.pad(marker, 100, constant_values=255)
_, ids, _ = detector.detectMarkers(padded)
assert ids is not None and ids.flatten().tolist() == [0]
c = page(out/'marcador-aruco-5cm-a4.pdf', 'Marcador para seguimiento - ArUco ID 0', 'DICT_4X4_50 | El cuadrado exterior negro mide 50 x 50 mm')
c.drawImage(ImageReader(Image.fromarray(marker)), 80*mm, 135*mm, width=50*mm, height=50*mm)
c.setFont('Helvetica', 10)
c.drawCentredString(105*mm, 120*mm, 'Conservar el margen blanco. No recortar junto al borde negro.')
c.drawCentredString(105*mm, 112*mm, 'Esta hoja sirve para el seguimiento actual; no es el tablero de calibracion.')
footer(c); c.save()
print('Generated and verified:', out/'hoja-charuco-a4.pdf', out/'marcador-aruco-5cm-a4.pdf')

# Letter sheets use physical dimensions, never a scaled A4 page.
c = page(out/'hoja-charuco-carta.pdf', 'Calibracion de camara - ChArUco (Carta)', '5 x 7 cuadrados de 30 mm | Marcadores de 15 mm | DICT_4X4_50', letter)
c.drawImage(ImageReader(Image.fromarray(image)), letter[0]/2-75*mm, 40*mm, width=150*mm, height=210*mm)
c.setFont('Helvetica', 9)
c.drawCentredString(letter[0]/2, 32*mm, 'Tablero: 150 x 210 mm. Pegar sobre una base plana y rigida.')
footer(c); c.save()
c=page(out/'marcadores-aruco-2cm-carta.pdf', 'Marcadores ArUco de 2 cm (Carta)', 'DICT_4X4_50 | Cuadrado negro: 20 x 20 mm | Seleccionar 2 cm en la app', letter)
for marker_id in range(6):
    marker=cv2.aruco.generateImageMarker(dictionary,marker_id,400)
    _, found, _=detector.detectMarkers(np.pad(marker,80,constant_values=255))
    assert found is not None and found.flatten().tolist()==[marker_id]
    x=(40+(marker_id%3)*55)*mm; y=(190-(marker_id//3)*65)*mm
    c.drawImage(ImageReader(Image.fromarray(marker)),x,y,width=20*mm,height=20*mm)
    c.setFont('Helvetica',10);c.drawCentredString(x+10*mm,y-6*mm,f'ID {marker_id}')
c.drawCentredString(letter[0]/2,85*mm,'Recortar dejando al menos 5 mm de margen blanco alrededor.')
footer(c);c.save()

# A cross-shaped net: one tab for each pair of exterior edges that meet in 3D.
def neg(v): return tuple(-n for n in v)
def add(*vectors): return tuple(sum(v[i] for v in vectors) for i in range(3))
faces={(1,0),(0,1),(1,1),(2,1),(1,2),(1,3)}
orient={(1,1):((1,0,0),(0,1,0),(0,0,1))}; queue=[(1,1)]
while queue:
    x,y=queue.pop(0);u,v,n=orient[x,y]
    for dx,dy,axes in [(1,0,(neg(n),v,u)),(-1,0,(n,v,neg(u))),(0,1,(u,neg(n),v)),(0,-1,(u,n,neg(v)))]:
        other=(x+dx,y+dy)
        if other in faces and other not in orient: orient[other]=axes;queue.append(other)
seams={}
for face in sorted(faces):
    x,y=face;u,v,n=orient[face]
    for edge,(dx,dy,a,b) in enumerate([(0,-1,(0,0),(1,0)),(1,0,(1,0),(1,1)),(0,1,(1,1),(0,1)),(-1,0,(0,1),(0,0))]):
        if (x+dx,y+dy) in faces: continue
        ends=[add(n,u if px else neg(u),v if py else neg(v)) for px,py in (a,b)]
        key=tuple(sorted(ends));seams.setdefault(key,[]).append((face,edge,a,b))
assert len(seams)==7 and all(len(pair)==2 for pair in seams.values())
tabs={(pair[0][0],pair[0][1]) for pair in seams.values()}
for side_cm in (2,5):
    side=side_cm*10*mm;tab=min(6*mm,side*.18)
    c=page(out/f'cubo-papel-{side_cm}cm-carta.pdf',f'Cubo de papel - lado {side_cm} cm (Carta)','Linea continua: cortar | Linea discontinua: doblar | Pestanas: pegar',letter)
    ox=(letter[0]-3*side)/2;oy=(letter[1]-4*side)/2-5*mm
    for face in sorted(faces):
        x,y=face;x0=ox+x*side;y0=oy+y*side
        c.setFont('Helvetica',8);c.drawCentredString(x0+side/2,y0+side/2,str(sorted(faces).index(face)+1))
        for edge,(dx,dy,a,b) in enumerate([(0,-1,(0,0),(1,0)),(1,0,(1,0),(1,1)),(0,1,(1,1),(0,1)),(-1,0,(0,1),(0,0))]):
            ax=x0+a[0]*side;ay=y0+a[1]*side;bx=x0+b[0]*side;by=y0+b[1]*side
            shared=(x+dx,y+dy) in faces;has_tab=(face,edge) in tabs
            c.setDash(3,2) if shared or has_tab else c.setDash()
            c.line(ax,ay,bx,by);c.setDash()
            if has_tab:
                tx=(bx-ax)/side;ty=(by-ay)/side
                p=c.beginPath();p.moveTo(ax,ay);p.lineTo(ax+tx*tab+dx*tab,ay+ty*tab+dy*tab)
                p.lineTo(bx-tx*tab+dx*tab,by-ty*tab+dy*tab);p.lineTo(bx,by);c.drawPath(p)
    footer(c);c.save()
print('Letter calibration, 2 cm markers, and 2/5 cm paper cube nets generated.')

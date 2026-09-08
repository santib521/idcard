MKT HIS WebView Proof 0.2
=========================

สถาปัตยกรรม: ต้องมี 2 APK ติดตั้งพร้อมกัน
1. MKT SmartCard Agent เดิม
   - อ่าน USB/ชิปจาก ACR39U
   - ให้บริการ Local API ที่ 127.0.0.1:8181
2. MKT HIS Wrapper Proof (โปรเจกต์นี้)
   - เปิดหน้า HIS และวางปุ่ม Floating
   - ไม่อ่าน USB/ชิปเอง และไม่ใช้แทน Agent

Application ID ของ Wrapper คือ com.monkeytech.hiswrapper ซึ่งต้องแยกจาก
Application ID ของ MKT SmartCard Agent เพื่อให้ติดตั้งคู่กันได้โดยไม่เกิด Package conflict

วัตถุประสงค์
- ทดสอบเปิด Ease HIS เต็มหน้าจอบน Android Tablet
- มีเพียงปุ่ม "อ่านบัตร" ลอยมุมขวาล่าง
- ปุ่มไม่หายเมื่อเปลี่ยนหน้าใน Domain easehospital.com
- ไม่แก้ Source Code ของ HIS
- ไม่แก้ OCR หรือ Card Reader ในหน้า MKT SmartCard

URL ที่กำหนดไว้
- HIS: https://his-uat.easehospital.com/web/login
- Floating: https://santib521.github.io/idcard/index.html

วิธีเปิดใน Android Studio
1. ใช้ Android Studio รุ่นปัจจุบัน และ JDK 17
2. เลือก Open แล้วเลือกโฟลเดอร์ MKT-HIS-WebView-Proof
3. รอ Gradle Sync และติดตั้ง Android SDK 35 หากโปรแกรมร้องขอ
4. ต่อ Tablet เปิด Developer options และ USB debugging
5. กด Run เพื่อทดสอบ หรือ Build > Build APK(s)

สิ่งที่ต้องติดตั้งใน Tablet ก่อนทดสอบ
- MKT SmartCard Agent ที่ใช้งานได้อยู่แล้ว (ห้ามถอนออก)
- ต่อเครื่องอ่าน ACR39U และอนุญาต USB
- อัปเดต Android System WebView/Chrome

พฤติกรรมที่ต้องเห็น
1. เปิด App แล้วเจอหน้า Login ของ Ease HIS ทันที
2. ไม่มีแถบ URL หรือปุ่มทดสอบอื่น
3. เห็น Floating จาก index.html เดิมลอยมุมขวาล่าง รูปและ Function ไม่ถูกแก้
4. Login และเปลี่ยนหน้า HIS แล้วปุ่มยังอยู่
5. กดรูปบัตรบน Floating แล้วอ่านบัตรทันทีเหมือน index.html เดิม
6. เมื่อ index.html ขยายหรือย่อ Wrapper จะปรับพื้นที่ตามอัตโนมัติ
7. กด Floating ค้างเพื่อเปิด Setup เพิ่ม HIS URL ได้ 6 รายการและเลือก Default
8. เปลี่ยน Default/URL ได้โดยไม่ต้อง Build APK ใหม่

Regression Proof
[ ] HIS Login ได้
[ ] เมนูและหน้าจอ HIS ใช้งานเหมือนเดิม
[ ] Floating ไม่หายเมื่อ URL เปลี่ยนภายใต้ easehospital.com
[ ] Floating ไม่บัง HIS ยกเว้นปุ่มขนาดเล็ก
[ ] กล้องขอ Permission และเปิดได้
[ ] Browse file เปิดตัวเลือกไฟล์ได้
[ ] Local Agent 127.0.0.1:8181 ติดต่อได้
[ ] อ่าน Smart Card ได้เหมือนเดิม
[ ] ปุ่ม Back ทำงานถูกต้อง

ข้อจำกัดของ Proof
- ยังไม่ได้ทำ Digital Signing สำหรับแจก Production
- ยังไม่ได้ทำ MDM/Kiosk Mode
- ลิงก์ออกนอก easehospital.com จะเปิดด้วย App ภายนอก
- หาก HIS ใช้ Popup/new window บางรายการ ต้องเก็บเคสแล้วเพิ่ม WebChromeClient ในรอบ Production

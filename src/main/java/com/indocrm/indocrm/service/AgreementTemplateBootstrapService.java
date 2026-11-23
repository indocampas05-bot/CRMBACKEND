// FILE: src/main/java/com/indocrm/indocrm/service/AgreementTemplateBootstrapService.java
// REPLACES existing file entirely.
//
// Fixes:
//  1. Argument order bug (see AgreementTemplateService) that stored the
//     literal string "PDF" as contentHtml for both templates.
//  2. The old HTML was the raw per-page text extraction of the source PDFs,
//     which repeats the letterhead contact block ("89258 11993...",
//     "www.indocampas.com", "TIDAL NEO...") after almost every clause because
//     that block is printed on every physical page of the PDF. In HTML this
//     is now a single header/footer shown once, exactly like a letterhead —
//     the clause text itself is unchanged/unshortened from the source PDFs.
//  3. Every client-specific value called out in the requirements (agreement
//     date, school/client name & address, pincode, plan, fee, discount, GST,
//     net/total payable, student strength, authorized signatory, signature
//     date) is now a {{merge_field}} instead of a hard-coded figure, so
//     AgreementService can substitute the values entered on the Add
//     Agreement form / the values from the selected AgreementPlan.
package com.indocrm.indocrm.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10) // run after AgreementPlanService's constructor-time seeding
public class AgreementTemplateBootstrapService implements CommandLineRunner {

    private final AgreementTemplateService templates;

    public AgreementTemplateBootstrapService(AgreementTemplateService templates) {
        this.templates = templates;
    }

    @Override
    public void run(String... args) {

        // ================= DIGITAL MARKETING & ADMIN DASHBOARD AGREEMENT =================
        templates.createSystemTemplate(
                "Digital Marketing & Admin Dashboard Agreement",
                "DIGITAL_MARKETING",
                2, // bump this number whenever the HTML below changes
                """
<div class="agreement-letterhead">
  <div class="agreement-brand">INDOCAMPAS TECH <span>Private Limited</span></div>
  <div class="agreement-contact">
    89258 11993 / 8925811994 &nbsp;|&nbsp; www.indocampas.com<br/>
    TIDAL NEO - Thiruchitrambalam Koot Road, Tamil Nadu 605111
  </div>
</div>

<div class="agreement-body">
  <h2 class="agreement-title">DIGITAL MARKETING &amp; ADMIN DASHBOARD AGREEMENT</h2>
  <p>This Digital Marketing &amp; ADMIN DASHBOARD AGREEMENT is made on <b>{{agreement_date}}</b></p>

  <h3>BY AND BETWEEN</h3>
  <p><b>INDOCAMPAS TECHNOLOGIES</b>, having its Branch office at TIDEL NEO - Thiruchitrambalam-Koot Road,
  Tamil Nadu 605111 (hereinafter referred to as the &ldquo;Service Provider&rdquo;, which expression shall
  include its successors and assigns)</p>
  <h3>AND</h3>
  <p><b>{{client_name}}</b>, having its {{client_address}}, Pin code: {{pincode}}
  (hereinafter referred to as the &ldquo;Client&rdquo;, which expression shall include its management,
  trustees, successors and permitted assigns)</p>
  <p>The Service Provider and Client are collectively referred to as the &ldquo;Parties&rdquo;.</p>

  <h3>1. SCOPE OF SERVICES</h3>
  <p>The Service Provider shall provide:</p>
  <ul>
    <li>Social Media Management (Facebook, Instagram, YouTube, WhatsApp Promotions)</li>
    <li>Meta Ads Campaign Management</li>
    <li>Admission Promotion Campaigns</li>
    <li>Creative Designing &amp; Content Creation</li>
    <li>Access to Admission Management Dashboard (No Cost)</li>
  </ul>

  <h3>2. TERM</h3>
  <p>Minimum contract period: 1 (one Year).<br/>
  Agreement auto-renews unless 30 days written termination notice is given.<br/>
  Early termination before minimum period requires payment of remaining contract fees.</p>

  <h3>3. SERVICE FEES</h3>
  <table class="agreement-fee-table">
    <tbody>
      <tr><td>Selected Plan</td><td>{{plan_name}}</td></tr>
      <tr><td>Yearly Professional Fee</td><td>&#8377;{{yearly_fee}}</td></tr>
      <tr><td>Special Discount</td><td>&#8377;{{discount_amount}}</td></tr>
      <tr><td><b>Net Payable Fee</b></td><td><b>&#8377;{{net_payable}}</b></td></tr>
    </tbody>
  </table>
  <p>Exclusive of GST: {{gst_percent}}%.</p>
  <p>Payment Terms:</p>
  <ul>
    <li>2% monthly interest on delayed payments</li>
    <li>Services may be suspended for non-payment</li>
  </ul>
  <p>Advertisement budget is separate and payable by Client.</p>

  <h3>5. ADMISSION DASHBOARD (NO COST)</h3>
  <p>5.1 The Service Provider shall provide its proprietary Admission Management Dashboard free of cost
  during contract validity.</p>
  <p>5.2 The Dashboard shall record all leads, enquiries, applications, and admissions.</p>
  <p>5.3 Dashboard data logs shall be final and legally binding for admission count.</p>

  <h3>6. PER-ADMISSION COMMISSION STRUCTURE</h3>
  <p>6.1 The Client agrees to pay: &#8377;2,000 per Confirmed Admission generated through Dashboard.</p>
  <p>6.2 If Client subscribes to ERP Software of Service Provider: Commission reduced to &#8377;1,000 per
  Confirmed Admission.</p>
  <p>6.3 &ldquo;Confirmed Admission&rdquo; means student completes admission process and pays admission/term
  fees.</p>
  <p>6.4 Commission invoices raised monthly and payable within 5 days.</p>
  <p>6.5 2% monthly interest on delayed commission payment.</p>

  <h3>7. NON-BYPASS &amp; ANTI-MANIPULATION CLAUSE</h3>
  <p>7.1 Client shall not bypass, suppress, manipulate, or process Dashboard leads outside system.</p>
  <p>7.2 Any admission generated through campaigns must be entered in Dashboard.</p>
  <p>7.3 Attempt to conceal admissions to avoid commission is material breach.</p>
  <p>7.4 Service Provider reserves right to audit admission records.</p>

  <h3>9. INTELLECTUAL PROPERTY</h3>
  <p>All creatives, strategies, systems, dashboard technology remain exclusive property of Service Provider.
  Usage rights granted only upon full payment.</p>

  <h3>10. CONFIDENTIALITY</h3>
  <p>Both parties agree to maintain confidentiality of:</p>
  <ul>
    <li>Student data</li>
    <li>Marketing strategy</li>
    <li>Financial details</li>
    <li>Dashboard system</li>
  </ul>
  <p>This clause survives termination.</p>

  <h3>12. FORCE MAJEURE</h3>
  <p>No liability for delays caused by:</p>
  <ul>
    <li>Government regulations</li>
    <li>Platform policy changes</li>
    <li>Natural disasters</li>
    <li>Technical disruptions</li>
  </ul>

  <h3>13. TERMINATION</h3>
  <p>30 days written notice required. Advance payments are non-refundable. Commission payable for all leads
  generated during contract period, even if admission confirmed after termination.</p>

  <h3>14. NON-COMPETE &amp; NON-SOLICITATION</h3>
  <p>During agreement period and 12 months thereafter: Client shall not copy, replicate, or engage third
  party to duplicate Dashboard system structure.</p>

  <h3>16. ENTIRE AGREEMENT</h3>
  <p>This Agreement supersedes all prior communications.</p>

  <h3>SIGNATURES</h3>
  <div class="agreement-signatures">
    <div class="agreement-signature-block">
      <p>For <b>INDOCAMPAS TECHNOLOGIES</b></p>
      <p>Authorized Signatory</p>
      <p>Name: __________________</p>
      <p>Signature: ______________</p>
      <p>Date: {{signature_date}}</p>
    </div>
    <div class="agreement-signature-block">
      <p>For <b>{{client_name}}</b></p>
      <p>Authorized Signatory</p>
      <p>Name: {{authorized_person_name}}</p>
      <p>Designation: {{authorized_person_designation}}</p>
      <p>Signature: ______________</p>
      <p>Date: {{signature_date}}</p>
    </div>
  </div>
</div>
""");

        // ================= ERP AGREEMENT =================
        templates.createSystemTemplate(
                "School ERP / Web Dashboard Agreement",
                "ERP",
                2, // bump this number whenever the HTML below changes
                """
<div class="agreement-letterhead">
  <div class="agreement-brand">INDOCAMPAS TECH <span>Private Limited</span></div>
  <div class="agreement-contact">
    9944213425 / 7339547987 &nbsp;|&nbsp; 89258 11993 / 8925811994 &nbsp;|&nbsp; www.indocampas.com<br/>
    44, Unicorn Plaza, Kamarajar Salai, Karaikal, Pondicherry-609602<br/>
    TIDAL NEO-Thiruchitrambalam Koot Road, Tamil Nadu 605111
  </div>
</div>

<div class="agreement-body">
  <h3>BETWEEN</h3>
  <p><b>INDOCAMPAS TECH PRIVATE LIMITED</b>, a company incorporated under the Companies Act, 2013, having
  its registered office at No. 44, Unicon Plaza 2nd Floor, Kamarajar Salai, Karaikal &ndash; 609602.
  Hereinafter referred to as the &ldquo;Service Provider&rdquo; (which expression shall, unless repugnant to
  the context, mean and include its successors, assigns, and representatives)</p>

  <h3>AND</h3>
  <p><b>{{client_name}}</b>, a private educational institution, having its address at {{client_address}},
  Pin code: {{pincode}}, hereinafter referred to as the &ldquo;Educational Institution&rdquo; (which
  expression shall, unless repugnant to the context, mean and include its administrators, representatives
  and assigns).</p>

  <h3>1. PURPOSE OF THE AGREEMENT</h3>
  <p>The Service Provider agrees to provide, and the Client educational institutes agrees to use, a
  cloud-based educational institutes management web dashboard and mobile application (&ldquo;Services&rdquo;)
  developed and maintained by Indocampas Tech Pvt. Ltd., for educational institutes management and
  communication purposes.</p>
  <p>The following terms and conditions constitute a permit agreement (&ldquo;Terms and Conditions&rdquo; or
  &ldquo;Permit Agreement&rdquo; or &ldquo;Terms of Use&rdquo;) between you (&ldquo;You&rdquo; or
  &ldquo;Your&rdquo; or &ldquo;User&rdquo; or &ldquo;Permittee&rdquo;) and Provider. By clicking the
  &lsquo;Accept&rsquo; or similar options and registering and subscribing to the Software as a Service SAAS
  Product, the Permittee agrees to this Permit Agreement along with the Subscription Agreement. The Permit
  Agreement shall be read along with the Additional Documents and in case of any inconsistency, the terms
  and conditions of the Permit Agreement shall prevail.</p>

  <h4>I. Overview</h4>
  <h4>A. Framework</h4>
  <ul>
    <li>Only Indocampas provides digital framework for self-management all private educational institutes
    other than Syllabus through this application.</li>
    <li>The framework includes modules for student management, academic planning and performance tracking
    and parents&rsquo; response and education-oriented activity.</li>
    <li>Indocampas tech Pvt Ltd Company has developed SAAS Product for private educational institutes in
    four categories listed below: 1. Super admin 2. Educational institution admin 3. Teachers&rsquo;
    application 4. Parent and student application</li>
  </ul>
  <p><b>User Role-Based Views:</b> Admin | Teacher | Student | Parent</p>

  <p><b>Quick Stats Panel:</b> Total Students / Teachers / Classes (View data); Today's Attendance (student +
  staff); Upcoming Events (View only); Notifications (Assignments, Circulars)</p>

  <h4>B. Modules</h4>
  <p><b>a. Academic Management</b> &ndash; Class &amp; Section Management, Subject Allocation, Timetable
  Generator (Class &amp; Exam timetables)</p>
  <p><b>b. Student Information System (SIS)</b> &ndash; Student Profiles, Enrolment Management</p>
  <p><b>c. Teacher Management</b> &ndash; Staff Directory, Teacher Timetables, Leave Requests &amp;
  Approvals, Performance Evaluation (feed &amp; monitor staff attendance)</p>
  <p><b>d. Examination &amp; Assessment</b> &ndash; Exam Scheduler, Mark Entry &amp; Grade book, Result
  Analysis</p>
  <p><b>e. Attendance Management</b> &ndash; Daily Attendance (Student), Daily Attendance (Staff)</p>
  <p><b>f. Fees &amp; Finance</b> &ndash; Fee Structure Setup, Online Payments Gateway Integration, Dues
  &amp; Discount Management, Invoice Generator, Financial Reports &amp; Ledgers</p>
  <p><b>g. Communication &amp; Notifications</b> &ndash; SMS/Email/Push Notifications, Notice Board (not in
  Pro Plus), Chat Interface (monitored)</p>
  <p><b>i. Canteen / Hostel / Transport</b> &ndash; Van-notifications only, no live tracking</p>
  <p><b>j. Assignments &amp; Homework</b> &ndash; Upload Homework (text &amp; photos), Submission Tracker</p>

  <h4>C. User Management &amp; Security</h4>
  <p>Role-Based Access Controls (RBAC); Login History &amp; Session Logs</p>
  <h4>D. Reports &amp; Analytics</h4>
  <p>Attendance Reports; Fee Collection Reports (print)</p>
  <h4>E. Settings &amp; Configuration</h4>
  <p>Educational institutes information (update institution profile)</p>
  <h4>F. Key Components</h4>
  <ul>
    <li>Education institutes internal accounts management through admin dashboard</li>
    <li>Teachers and students timetable for examination and classes</li>
    <li>Digital attendance for teachers and students</li>
    <li>Tracking of van and students (attendance only)</li>
    <li>Parent App supports managing up to 5 children (same/different schools)</li>
  </ul>
  <h4>G. Benefits</h4>
  <p>Streamlined operations, data-driven insights, enhanced collaboration (chat interface monitored for
  safety).</p>
  <h4>H. Customization</h4>
  <p>This INDOCAMPAS tech application cannot be customized individually. INDOCAMPAS tech application is
  divided into three categories for educational institutions: 1. PRO, 2. PRO PLUS, 3. PRO MAX.</p>

  <h3>2. SCOPE OF SERVICES</h3>
  <p>The Services provided by the Provider through its Play store or app store or Website are
  (&ldquo;Services&rdquo; or &ldquo;SAAS Product&rdquo;). The Provider has developed a cloud-based platform,
  which it makes available to Permittee, used for maintaining records and facilitating all processes of the
  Institutions from admission of new students to graduation and thereafter, including classroom and
  examination timetables, attendance, examination details/results, hostel information, lunch menu
  information, library information, transportation, events, student fee information/collection/pending
  information and loan reference information, and any other services provided by Provider from time to
  time.</p>
  <p>The Services provided shall include: Student Information Management (profile, admission/enrolment,
  tracking, performance monitoring); Academic Management (class/section, subject/syllabus, exam/fees/
  assessment, grade/report card); Teacher and Staff Management (profile, class/subject assignment,
  attendance/leave, role-based access, sub-profiles); Parent and Student Engagement (parent-teacher
  communication, student portal, parent portal, PTM); Administrative Management (fee management, inventory,
  transport, hostel); Reporting and Analytics (dashboards, customizable reports, export in PDF/Excel/CSV,
  attendance &amp; fee collection reports); Integration and Security (third-party integration, data
  security/backup, access control); Support and Maintenance (updates, support via email/chat/phone, training
  resources).</p>
  <p><b>Services Not Included:</b> on-premise installation; customization beyond the standard feature set
  (available via separate contracts); hardware provisioning; data migration beyond initial onboarding
  (additional charges); legal/financial/educational consulting services.</p>
  <p><b>Service Availability:</b> INDOCAMPAS aims for high uptime but does not guarantee uninterrupted
  access; performance depends on the user's internet connectivity and device compatibility.</p>

  <h3>3. TERM</h3>
  <p>This Agreement shall be valid for an initial period of 12 months (Subscription) commencing from the
  date of signing and may be renewed upon mutual agreement.</p>
  <p><b>3.1 Initial Term:</b> Commences on the Effective Date and continues for one (1) year unless
  terminated earlier.</p>
  <p><b>3.2 Renewal:</b> Automatically renews for successive one (1) year terms unless either party gives 30
  days&rsquo; written notice of non-renewal.</p>
  <p><b>3.3 Termination:</b> Either party may terminate for convenience with 30 days&rsquo; written notice
  after the Initial Term, or for cause if the other party materially breaches and fails to cure within 30
  days.</p>

  <h3>4. FEES AND PAYMENT TERMS</h3>
  <p><b>4.1 Trial Terms:</b> A 600 hours / 24-day trial period applies; extensions may be requested and are
  granted at INDOCAMPAS's discretion. Once converted to a paying account, billing begins immediately.</p>
  <p><b>4.2 Subscription Billing:</b> Charged annually depending on the selected plan; billed in advance;
  no refunds/credits for partial year, upgrade/downgrade, or unused months except at Company Management's
  discretion. Billing is based on students enrolled on the bill date (left/deleted students excluded). All
  fees are exclusive of applicable taxes.</p>

  <table class="agreement-fee-table">
    <caption><b>ERP Software Subscription Charges</b> (Student Strength: {{student_strength}})</caption>
    <tbody>
      <tr><td>Selected Plan</td><td>{{plan_name}}</td></tr>
      <tr><td>ERP Software Subscription Fee</td><td>&#8377;{{subscription_fee}}</td></tr>
      <tr><td>GST @ {{gst_percent}}%</td><td>&#8377;{{gst_amount}}</td></tr>
      <tr><td><b>Total Amount Payable</b></td><td><b>&#8377;{{total_payable}}</b></td></tr>
    </tbody>
  </table>

  <p><b>4.3 Mode of Payment:</b> Credit/Debit Cards, UPI/Net Banking (India only), RazorPay, Direct Bank
  Transfer (Enterprise). Payment processing via secure third-party gateways.</p>
  <p><b>Late Payment:</b> If the School fails to pay by the due date, the Service Provider may charge 2%
  monthly interest, suspend/restrict access, and terminate accounts after 15 days of non-payment without
  further notice.</p>
  <p><b>4.5 Taxes:</b> All fees are exclusive of applicable taxes, borne by the School as per prevailing
  laws.</p>
  <p><b>4.6 Fee Revision:</b> Fees may be revised annually with 30 days&rsquo; written notice.</p>
  <p><b>4.7 Refund Policy:</b> Fees are non-refundable except for Service Provider-attributable service
  failure, refunded pro-rata.</p>

  <h3>5. OBLIGATION OF THE SERVICE PROVIDER</h3>
  <p>Provide the web dashboard 24/7 (except scheduled maintenance); install/configure and complete initial
  setup within the agreed timeline; provide training and ongoing technical support (email/phone/chat);
  resolve technical issues within defined timeframes; ensure data security and confidentiality; provide
  regular updates/maintenance with prior notice; comply with applicable laws; warrant the dashboard functions
  substantially per documentation and rectify failures at no additional cost.</p>

  <h3>6. OBLIGATION OF THE CLIENT SCHOOL</h3>
  <p>Provide timely access to data/staff/infrastructure; designate a point of contact; pay fees on time and
  raise invoice disputes promptly; use the dashboard only for internal school purposes; prevent unauthorized
  access and misuse; provide required hardware/internet bandwidth; ensure data accuracy and prompt updates;
  maintain confidentiality of credentials and proprietary information; ensure lawful and ethical use.</p>

  <h3>7. CONFIDENTIALITY</h3>
  <p>Confidential Information includes student/staff/administrative data, financial information and
  pricing, technical details/code/specifications, business strategies/reports/communications, and
  usernames/passwords/system access details, subject to standard exceptions (public availability, lawful
  third-party receipt, independent development, legally compelled disclosure). Obligations survive
  termination for three (3) years; each party shall return or destroy Confidential Information upon
  termination (archival/legal copies excepted).</p>

  <h3>8. INTELLECTUAL PROPERTY RIGHTS</h3>
  <p>All rights in the web dashboard and mobile application remain the sole property of Indocampas Tech Pvt.
  Ltd. The Service Provider grants the School a non-exclusive, non-transferable, non-sub-licensable, limited
  license to use the dashboard for internal educational/administrative purposes during the term. The School
  shall not copy, reverse engineer, resell, or remove proprietary notices. All School data (students, staff,
  academic, operational) remains the sole property of the School. Custom features/modules/reports developed
  on written request may be Service-Provider-owned with a perpetual license to the School, or jointly owned/
  licensed as mutually agreed in writing before development. The Service Provider warrants no third-party IP
  infringement and will defend/indemnify the School against such claims at its own expense.</p>

  <h3>9. TERMINATION</h3>
  <p>Either party may terminate for convenience (30 days&rsquo; notice, after Initial Term) or for cause
  (30 days&rsquo; notice for uncured material breach, insolvency, or unlawful/unethical conduct). The
  Service Provider may terminate immediately without notice for non-payment beyond a 15-day grace period, or
  evidence of misuse/tampering/unauthorized access. On termination: the School stops using the dashboard;
  outstanding dues become payable within 15 days; a final data export is provided within 30 days on written
  request; Confidential Information is returned/destroyed. Confidentiality, IP, Limitation of Liability, and
  Governing Law survive termination.</p>

  <h3>10. LIMITATION OF LIABILITY</h3>
  <p>Neither party is liable for indirect, incidental, special, punitive or consequential damages. Except
  for gross negligence, willful misconduct, breach of confidentiality, or IP violation, aggregate liability
  is capped at fees paid by the School in the preceding twelve (12) months. The Service Provider is not
  liable for data loss from factors beyond its reasonable control, nor does it warrant uninterrupted,
  error-free service. These limitations are an essential basis of the bargain.</p>

  <h3>11. FORCE MAJEURE</h3>
  <p>Neither party is liable for delay/failure due to natural disasters, epidemics/pandemics, government/
  legal acts, war/riots/civil disturbance, cyber-attacks/internet outages, labor strikes, or power/utility
  disruptions. The affected party must promptly notify the other and mitigate impact; obligations are
  suspended for the event's duration; either party may terminate with 15 days&rsquo; notice if the event
  exceeds 30 consecutive days, without liability.</p>

  <h3>12. DISPUTE RESOLUTION</h3>
  <p>Disputes are first addressed through good-faith negotiation within 15 days, then mediation within 30
  days if unresolved. If mediation fails, disputes are finally resolved by binding arbitration under the
  Arbitration and Conciliation Act, 1996, by a sole arbitrator, venue Puducherry, in English, with a final
  and binding decision. This Agreement is governed by the laws of India; courts of Puducherry have exclusive
  jurisdiction subject to the arbitration clause.</p>

  <h3>13. ENTIRE AGREEMENT</h3>
  <p>This Agreement, including all annexures, schedules and attachments, constitutes the entire agreement
  between Indocampas Tech Private Limited and the Client School and supersedes all prior discussions,
  understandings, proposals, negotiations, representations and agreements, oral or written. No amendment,
  modification or waiver is valid unless in writing and signed by both parties.</p>

  <h3>IN WITNESS WHEREOF</h3>
  <p>the parties have executed this Agreement on the date first written above.</p>
  <div class="agreement-signatures">
    <div class="agreement-signature-block">
      <p>For <b>INDOCAMPAS TECH Pvt. Ltd.</b></p>
      <p>Signature: ______________</p>
      <p>Name: __________________</p>
      <p>Chief Executive Officer</p>
      <p>Date: {{signature_date}}</p>
    </div>
    <div class="agreement-signature-block">
      <p>For: <b>{{client_name}}</b></p>
      <p>Signature: ______________</p>
      <p>Name: {{authorized_person_name}}</p>
      <p>Designation: {{authorized_person_designation}}</p>
      <p>Date: {{signature_date}}</p>
    </div>
  </div>
</div>
""");
    }
}

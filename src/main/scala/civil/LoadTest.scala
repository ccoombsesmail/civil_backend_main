 package civil
 import civil.directives.OutgoingHttp.createClerkUser
 import zio._
 import civil.models._
 import civil.repositories.QuillContext

 import java.util.UUID
 import java.time.LocalDateTime
// import civil.directives.SentimentAnalyzer
 import civil.models.ClerkModels.CreateClerkUser

 import scala.concurrent.ExecutionContext.Implicits.global
 import scala.util.{Failure, Success}

 object LoadTest extends zio.App  {
//   val comments = List("Coronavirus Agenda: \"It Was Planned Years In Advance\" - \"The final frontier - they want to conquer our body- they w… https://t.co/vzpZ3w5SHB","#VPS20 #TERAVOLT is an impressive, global registry led by @marinagarassino @HornLeora including only patients with… https://t.co/vQHpehOuw7","#AirIndiaExpress |   About 150 persons, who took part in the rescue operations, are now under quarantine as one of… https://t.co/oLYhATU6im","#VPS20 Presidential symposium overview of #COVID19 and thoracic cancers by @marinagarassino. First case in Italy re… https://t.co/9odLnz77wb","#COVID19 on the rise&gt;&gt;&gt;Sturgis Motorcyle Rally Bikers Arrive by Thousands, Masks and Distancing Rare via @TMZ… https://t.co/6cYugJtZze","Kaspersky: Covid-19 has created a “perfect storm” for cybercriminals https://t.co/JaMf7Io8MR #kaspersky… https://t.co/2GSVgtmEKw","Disturbed by incidences of #Doctors not receiving proper beds and treatments after getting infected with #Covid19,… https://t.co/wCgOFPzJI4","Find out about how @UCL and its partners are working to better understand the impacts of #COVID19 and aid recovery… https://t.co/NqiqBbCO5D","Survivors of #Covid19 show increased rate of psychiatric disorders. Research suggests more than half experience PTS… https://t.co/WcnfedTFte","Choosing just the right mask to wear this #Caturday. #StaySafe #masks #SocialDistancing #COVID19  #cats #pets… https://t.co/7O5qkouSOw","@LGBTResistance2 We cannot rely upon #trump to #ProtectOurKids or #PortectOurTeachers from #coronavirus they are hi… https://t.co/ozl5PiQ5B8","3 new cases in Iceland   [12:51 GMT] #coronavirus #CoronaVirusUpdate #COVID19 #CoronavirusPandemic","Join us for a Facebook Live session with Dr Mohit (Orthopedic) on 14 August 2020 at 1 pm. Our expert doctor will be… https://t.co/vOxTo0EHmS","As 🍄rump Pushes to Reopen  Schools Many #Kids and #Teachers Will Get #Covid19  Some Will Die....💔💔💔… https://t.co/c34xlPazfU","Some people are not handling well the change in society the pandemic created. They can’t adapt 😐 — Sturgis Motorcyc… https://t.co/Oja9FUJn3A","Even before #COVID19, families needed options for older adults needing long term care. Accessory dwelling units lik… https://t.co/X33FdsK3RV","Amid the #COVID19 outbreak, many of you are now working as #VirtualTeams. #RemoteWork has it's challenges and can e… https://t.co/wt8uJGAF2O","@SturgisRally Hey let anyone know who comes down w/ #COVID19 they can “WILL”their Motorcycle 🏍 2 me! I will take ca… https://t.co/PGr3Ozm4fR","@riseupmelbourne Is this #covid19 pay to play?","@KamalaHarris @JoeBiden @SenKamalaHarris You're DELUSIONAL.  @TheDemocrats have destroyed the entire country by pol… https://t.co/p8fpOkSEwt","All first-year students selected for 2020 admission to the Royal University of #Bhutan (RUB) colleges will have to… https://t.co/2dVShI0dc0","Local arts programs help build an increased social cohesion throughout communities. Many participatory arts program… https://t.co/zxTNkSvoQN","It depends on how extrovert/introvert one is and how adventurous or quiet of character. #NewWorldOrder… https://t.co/9hSyEcgNt1","@diegojimmyjames We cannot rely upon #trump to #ProtectOurKids or #PortectOurTeachers from #coronavirus they are hi… https://t.co/OmDklbfU8n","Philadelphia Officials: Answer Your Phone If A 215-218-XXXX Number Calls You  — it’s a #COVID19 contact tracer https://t.co/8kRb3rwkHM","#CoronaInfoCH #COVID19 #corona #tech #us  Trump’s China Tech Attack is Bad Ne... https://t.co/42WBpccv3P","@Acosta The most horrifying: the press conference “rally” used a national platform to UNDERMINE public health respo… https://t.co/haNc1y2UZ1","1/ Ridiculous protest.  Spoke to one attendee who told me:   - It’s not a pandemic - gave me incorrect definition -… https://t.co/EicZbbBUHP","I’m on a bit of a songwriting roll! Two #covidinspired songs in two days...the other one is a dance song....you’ll… https://t.co/BngY0d5YkA","#Brexit is not only going to pile economic havoc on top of the devastation of #COVID19, but it also has triggered a… https://t.co/qCUdo3k3UQ","No evidence that receipt of anti-cancer therapy (chemotherapy, targeted therapy, endocrine therapy and immunotherap… https://t.co/mW2Rogu7LM","Yes my university will now be online BUT can we talk about how I was preparing myself to go back in the class even… https://t.co/twDX7WSZ3r","Here're details of 274 new #COVID19 positive cases reported from #Ganjam district today   Total cases: 12,633 Activ… https://t.co/vawyEfzJ7F","Our team of sales consultants are ready to support both our existing and new customers. During #COVID19 they are wo… https://t.co/CEkVPpUiCs","I could breathe. Double pneumonia and a blood oxygen level of 56.  #Death had me on his scythe. Seizure upon seize… https://t.co/GAfT1B2Alw","I'm the last person to praise China. Bastards hold Canadian hostages for 607 days.....  But a great idea is a great… https://t.co/PzUY3Tmy0B","Best 7 places to eat around the world 🌎 No. 1 will surprise 😳 you 👇🏻 https://t.co/FkHCDCYKIO —  #travel #food… https://t.co/vCsJ3b90a0","Video: What impact will the recession caused by #COVID19 have on the demand for data scientists? #DataScience #AI… https://t.co/ZbzVS3QvHD","Picture yourself in a boat on a river With tangerine trees, and marmalade skies. #jrleshinskyphoto… https://t.co/WEuQBKan4A","UPDATE: New cases of #coronavirus #COVID19 #COVID_19   Iran (+2125, tot. 324692) Nepal (+378, tot. 22592) Switzerla… https://t.co/SmSHc23CJv","ICN CEO #HowardCatton tells @SkyNews that #nurses around the world deserve a pay rise - on day UK nurses hold mass… https://t.co/5AyldkYYRm","Yesterday I posted this on #instagram and somebody asked if I was trying to say there's going to be a second wave.… https://t.co/f4ZhipeS0P","Hospitals leaked personal details of #COVID19 patients on unencrypted system  A #security researcher reveals that h… https://t.co/AM1BfjzVxX","When it comes to #HealthData, sharing is caring. Last month, @WHO brought together the global scientific community… https://t.co/9u4ItR5Z0u","@crean_fr @BVMConsolatrix And 2020 May very well be the results of pachamama @Pontifex Sad times! #COVID19… https://t.co/tkNNRnKqfl","@Anons_daddyO We cannot rely upon #trump to #ProtectOurKids or #PortectOurTeachers from #coronavirus they are his p… https://t.co/dOM9cwsehQ","Important advice on how to keep cool and safe during the these hot days!   #beattheheat #COVID19 #StaySafe https://t.co/6wgYmlrHkR","Covid-19 brings back plan for credit enhancement NBFC  https://t.co/mgeLu2DZna  #NBFC #Industry #Government #COVID19 https://t.co/bElSTp9CB7","Hi Katherine, I enjoyed reading your piece on “Why the Coronavirus is More Likely to ‘Superspread’ Than the Flu”. I… https://t.co/VTXocDlmzF","Iran has one of the highest fatality rates from #COVID19  Regime has done nothing to protect its citizens and has d… https://t.co/S1D4Tmw6Om","Workers prepare graves at a municipal cemetery for the coronavirus (#COVID19) death in the capital of El Salvador… https://t.co/leAP9qKQOH","#Coronavirus: 2 employees of #Tokyo Olympic organizers positive for #COVID19 - National | @Globalnews https://t.co/1ZB9DNlQ6c","So is any firm out there planning to do cell phone tracking (like they did to S. Florida spring breakers) to watch… https://t.co/6Bwfhpus3M","@DanielAndrewsMP will you share the data that was used to show that 6 weeks curfew was sufficient to handle this… https://t.co/DsIXc7qBPg","#COVID19 hits grim milestone 160k dead and Dems go political blaming trump. Hiroshima 75 yrs ago, DEMOCRAT Harry Tr… https://t.co/d23hI960V6","@SeanRamones We cannot rely upon #trump to #ProtectOurKids or #PortectOurTeachers from #coronavirus they are his pa… https://t.co/zqJWkSvoIF","I love seeing two young guys listening. Yes, listening intently to a piece of music. #Bravo #PhilCollins shocking t… https://t.co/0Z8ATQJW9X","Looks like after several weeks of insignificant numbers of #COVID19 cases, Sachsen is back in the game... https://t.co/HyX5YPGYEB","Finacle: In recent months, several banks that were looking to go #digital in a phased manner before #COVID19 have c… https://t.co/arwbFDT4JG","#Now  Queen https://t.co/9WAZzyNVfR Breakthru  #UniversalSuffrage #coronavirus #citizen&gt;#consumerism&gt;#consumer… https://t.co/aJR0uB8Bqv","2020-08-08 11:49:17","Girls and young women from marginalised communities are likely to be affected by secondary impacts of #COVID19 like… https://t.co/N8pXOnb5CR","#COVID19  To some of those reckless “invincibles”out there:  Corona patient, 28, didn’t think she was at risk: “I w… https://t.co/v3lvgF3RrC","#BREAKING: Vietnam's National Steering Committee have reported 21 new cases of #COVID19, bringing the total to 810.… https://t.co/sDw4HOBWrE","Iran's human rights defender Narges Mohammadi is showing suspected #COVID19 symptoms while in Jail. The authorities… https://t.co/ehgcaE0ZZj","Myself and just 2 other people wearing masks-the rest are like live and let live bro, right on #covid19 #lockdown… https://t.co/WNT98WBXhi","@MarkMeadows is terrified. Scared to death. He has held up Americans #COVID19 financial relief to try and make… https://t.co/k9r8Nr1x1o","Lockdown-hit Preston deploys ‘don’t kill Granny’ message for young people #coronavirus #COVID19… https://t.co/zcWMmZnakt","2020-08-08 11:48:41","On a positive note, my retirement account has rebounded.  #coronavirus #COVID19 https://t.co/F3X684nimI","False","699 more people have tested positive of #COVID19 in Kenya. The total number of confirmed cases is 25,837.  5 people… https://t.co/5gEbpoTf5V","For the last zillion years Coronaviruses have hit in January/February. It is now Jan/Feb on the Southern hemisphere… https://t.co/r7pGRYlces","#Mediclaim is not useful for #cashless treatment for #covid19 patients. Private hospitals denying cashless treatmen… https://t.co/tyU87eVpUJ","True","#Bargarh district reports 7 new #COVID19 cases, tally reaches 583 #coronavirus #coronavirusinindia https://t.co/U6Rd8r0Ive")
//
//   val commentsObjects = comments.map(comment => {
//     val sentiment = SentimentAnalyzer.mainSentiment(comment)
//     Comments(
//       UUID.randomUUID(),
//       s"<p>$comment</p>",
//       "ccoombs",
//       sentiment.toString(),
//       UUID.fromString("6145bdb0-1151-4d54-8cff-aa3b981e8778"),
//       None,
//       LocalDateTime.now(),
//       2,
//       None,
//       None
//     )}
//   )

//   import QuillContext.ctx.{lift => qlift, run => qrun, _}

   override def run(args: List[String]) = {
     val runtime = zio.Runtime.default

     runtime.unsafeRun(for {
       res <- ZIO.fromFuture(_ => createClerkUser(CreateClerkUser()))
       _ = println(res)
     } yield ())
     ZIO.succeed().exitCode
//     createClerkUser(CreateClerkUser()).onComplete {
//       case Success(_) => {
//         println("hey")
//         ZIO.succeed(()).exitCode
//       }
//       case Failure(t) => {
//         println("An error has occurred: " + t.getMessage)
//         ZIO.succeed(()).exitCode
//       }
//     }
//       val a = quote {
//         liftQuery(commentsObjects).foreach(e => query[Comments].insert(e))
//       }
//
//       qrun(a)




   }
 }

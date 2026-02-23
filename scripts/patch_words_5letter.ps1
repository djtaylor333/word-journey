
$wordsPath = "c:\Users\david\OneDrive\Documents\projects\AI Projects\word-journey\app\src\main\assets\words.json"
$existing = Get-Content $wordsPath -Raw | ConvertFrom-Json

$set5 = @{}
foreach ($w in $existing."5") { $set5[$w.word] = $true }

$extra5 = @(
  [pscustomobject]@{word="HAPPY";definition="Feeling joy and contentment"},
  [pscustomobject]@{word="HARSH";definition="Unpleasantly rough or severe"},
  [pscustomobject]@{word="HAVEN";definition="A place of safety or shelter"},
  [pscustomobject]@{word="HEDGE";definition="A row of closely planted shrubs forming a boundary"},
  [pscustomobject]@{word="HENCE";definition="For this reason; from this point in time"},
  [pscustomobject]@{word="HIPPO";definition="A large semi-aquatic African mammal"},
  [pscustomobject]@{word="HORSE";definition="A large animal used for riding and work"},
  [pscustomobject]@{word="HOTEL";definition="A building where guests pay to stay overnight"},
  [pscustomobject]@{word="HOUSE";definition="A building where people live"},
  [pscustomobject]@{word="HUMAN";definition="A person; relating to people"},
  [pscustomobject]@{word="HUMOR";definition="The quality of being funny or amusing"},
  [pscustomobject]@{word="INNER";definition="Located inside or closer to the center"},
  [pscustomobject]@{word="IVORY";definition="The creamy white material from elephant tusks"},
  [pscustomobject]@{word="JEWEL";definition="A precious stone used in jewelry"},
  [pscustomobject]@{word="JUDGE";definition="A person who decides outcomes in a court or contest"},
  [pscustomobject]@{word="JUICE";definition="The liquid extracted from fruit or vegetables"},
  [pscustomobject]@{word="KNIFE";definition="A sharp blade with a handle for cutting"},
  [pscustomobject]@{word="KNOCK";definition="To hit a surface to make a sound"},
  [pscustomobject]@{word="KNOWN";definition="Recognized and understood by others"},
  [pscustomobject]@{word="LABEL";definition="A piece of paper giving information about something"},
  [pscustomobject]@{word="LASER";definition="A concentrated beam of amplified light"},
  [pscustomobject]@{word="LEMON";definition="A yellow citrus fruit with a sour taste"},
  [pscustomobject]@{word="LEVEL";definition="Flat and even; a stage of difficulty"},
  [pscustomobject]@{word="LOCAL";definition="Relating to a particular area or neighborhood"},
  [pscustomobject]@{word="LUCKY";definition="Having good fortune or success by chance"},
  [pscustomobject]@{word="MAGIC";definition="The power to make impossible things happen"},
  [pscustomobject]@{word="MANOR";definition="A large country house with land"},
  [pscustomobject]@{word="MAPLE";definition="A tree with broad leaves that produces sweet sap"},
  [pscustomobject]@{word="MARCH";definition="The third month; to walk in a steady rhythm"},
  [pscustomobject]@{word="MAYOR";definition="The elected head of a city or town"},
  [pscustomobject]@{word="MEDAL";definition="A metal disc given as an award for achievement"},
  [pscustomobject]@{word="MERCY";definition="Compassionate treatment shown to someone in distress"},
  [pscustomobject]@{word="METAL";definition="A hard shiny material like iron, gold, or silver"},
  [pscustomobject]@{word="MINOR";definition="Smaller or less important; not yet an adult"},
  [pscustomobject]@{word="MODEL";definition="A representation of something; a person who displays clothes"},
  [pscustomobject]@{word="MONEY";definition="Currency used to buy goods and services"},
  [pscustomobject]@{word="MOUNT";definition="To climb onto something; a mountain"},
  [pscustomobject]@{word="MOUSE";definition="A small furry rodent; a computer pointing device"},
  [pscustomobject]@{word="MUSIC";definition="Sound organized in rhythm, melody, and harmony"},
  [pscustomobject]@{word="NERVE";definition="A fiber that carries signals through the body; boldness"},
  [pscustomobject]@{word="NIGHT";definition="The time between sunset and sunrise when it is dark"},
  [pscustomobject]@{word="NOBLE";definition="Having high moral character; of aristocratic rank"},
  [pscustomobject]@{word="NOVEL";definition="An original idea; a long work of fiction"},
  [pscustomobject]@{word="NURSE";definition="A person trained to care for sick or injured people"},
  [pscustomobject]@{word="OCEAN";definition="A vast body of salt water covering most of the Earth"},
  [pscustomobject]@{word="OLIVE";definition="A small oval fruit from a Mediterranean tree"},
  [pscustomobject]@{word="ONION";definition="A round vegetable with a strong smell and taste"},
  [pscustomobject]@{word="ORBIT";definition="The curved path of an object moving around another"},
  [pscustomobject]@{word="OTHER";definition="Different from the one already mentioned"},
  [pscustomobject]@{word="OUTER";definition="Situated on the outside or far from the center"},
  [pscustomobject]@{word="PEACE";definition="Freedom from war, conflict, or disturbance"},
  [pscustomobject]@{word="PIANO";definition="A large keyboard instrument with strings struck by hammers"},
  [pscustomobject]@{word="PLACE";definition="A particular area or location"},
  [pscustomobject]@{word="PLANT";definition="A living organism that grows in soil; to put seeds in ground"},
  [pscustomobject]@{word="PLATE";definition="A flat dish used to serve food"},
  [pscustomobject]@{word="POWER";definition="The ability to do something; energy or strength"},
  [pscustomobject]@{word="PRICE";definition="The amount of money paid for something"},
  [pscustomobject]@{word="PRIDE";definition="A feeling of satisfaction in oneself or others"},
  [pscustomobject]@{word="PRIME";definition="Of the best quality; the most important"},
  [pscustomobject]@{word="PROVE";definition="To demonstrate that something is true"},
  [pscustomobject]@{word="RAPID";definition="Happening in a short time; very fast"},
  [pscustomobject]@{word="REACH";definition="To stretch out to touch or arrive at a place"},
  [pscustomobject]@{word="REALM";definition="A kingdom or a particular area of activity"},
  [pscustomobject]@{word="RIGHT";definition="Morally correct; on the side of the body with the dominant hand"},
  [pscustomobject]@{word="RIVER";definition="A large natural stream of water flowing toward the sea"},
  [pscustomobject]@{word="ROBOT";definition="A machine that can perform tasks automatically"},
  [pscustomobject]@{word="ROUND";definition="Shaped like a circle or ball"},
  [pscustomobject]@{word="ROYAL";definition="Relating to a king or queen"},
  [pscustomobject]@{word="RULER";definition="A person who governs; a flat strip for measuring"},
  [pscustomobject]@{word="SCALE";definition="A device for weighing; the hard plates on a fish"},
  [pscustomobject]@{word="SCENE";definition="A place where something happens; part of a play"},
  [pscustomobject]@{word="SCORE";definition="The number of points in a game; to achieve a goal"},
  [pscustomobject]@{word="SENSE";definition="Any of the five ways of experiencing the world"},
  [pscustomobject]@{word="SHAKE";definition="To move back and forth or up and down quickly"},
  [pscustomobject]@{word="SHAPE";definition="The form or outline of something"},
  [pscustomobject]@{word="SHARP";definition="Having a fine edge or point that cuts easily"},
  [pscustomobject]@{word="SHIFT";definition="To move from one place to another; a work period"},
  [pscustomobject]@{word="SHINE";definition="To give off bright light; to excel"},
  [pscustomobject]@{word="SLICE";definition="A thin flat piece cut from something"},
  [pscustomobject]@{word="SLIDE";definition="To move smoothly over a surface; a playground slope"},
  [pscustomobject]@{word="SMILE";definition="A happy expression showing your teeth"},
  [pscustomobject]@{word="SMOKE";definition="The grey gas produced when something burns"},
  [pscustomobject]@{word="SOLID";definition="Firm and hard; not liquid or gas"},
  [pscustomobject]@{word="SOUND";definition="Vibrations heard as audio; healthy and undamaged"},
  [pscustomobject]@{word="SPACE";definition="The area beyond Earth's atmosphere; an empty area"},
  [pscustomobject]@{word="SPEAK";definition="To say words; to communicate verbally"},
  [pscustomobject]@{word="SPEED";definition="The rate at which something moves"},
  [pscustomobject]@{word="STAND";definition="To be upright on your feet; a structure for support"},
  [pscustomobject]@{word="START";definition="To begin an activity or journey"},
  [pscustomobject]@{word="STATE";definition="A condition or situation; a region of a country"},
  [pscustomobject]@{word="SWEET";definition="Having a sugary pleasant taste"},
  [pscustomobject]@{word="SWIFT";definition="Moving very fast"},
  [pscustomobject]@{word="SWORD";definition="A weapon with a long sharp blade"},
  [pscustomobject]@{word="TIGER";definition="A large wild cat with orange and black stripes"},
  [pscustomobject]@{word="TIRED";definition="Feeling a need to rest or sleep"},
  [pscustomobject]@{word="TOAST";definition="Bread that has been browned by heat"},
  [pscustomobject]@{word="TOUCH";definition="To make contact with something using your hand"},
  [pscustomobject]@{word="TRACE";definition="A mark or sign left behind; to follow a path"},
  [pscustomobject]@{word="TRACK";definition="A path or course; to follow the trail of something"},
  [pscustomobject]@{word="TRADE";definition="The buying and selling of goods"},
  [pscustomobject]@{word="TRAIN";definition="A series of connected vehicles on a railway; to practice"},
  [pscustomobject]@{word="TRUST";definition="A firm belief in the reliability of someone"},
  [pscustomobject]@{word="TULIP";definition="A colorful cup-shaped spring flower"},
  [pscustomobject]@{word="UNDER";definition="Below or beneath something else"},
  [pscustomobject]@{word="UNITE";definition="To come together as one group"},
  [pscustomobject]@{word="UNTIL";definition="Up to the time when something happens"},
  [pscustomobject]@{word="UPPER";definition="Located at a higher level or position"},
  [pscustomobject]@{word="WATCH";definition="To look at something carefully; a small clock worn on the wrist"},
  [pscustomobject]@{word="WATER";definition="A clear liquid essential for life"},
  [pscustomobject]@{word="WEAVE";definition="To interlace threads to make fabric"},
  [pscustomobject]@{word="WEDGE";definition="A triangular piece used to split things apart"},
  [pscustomobject]@{word="WHALE";definition="The largest ocean mammal"},
  [pscustomobject]@{word="WORLD";definition="The Earth and everything on it"},
  [pscustomobject]@{word="WORRY";definition="To feel anxious about something uncertain"},
  [pscustomobject]@{word="WORTH";definition="The value of something; deserving of"},
  [pscustomobject]@{word="WRIST";definition="The joint connecting the hand to the arm"}
)

$newEntries = @()
$addedCount = 0
foreach ($entry in $extra5) {
    if (-not $set5.ContainsKey($entry.word)) {
        $newEntries += $entry
        $set5[$entry.word] = $true
        $addedCount++
    }
}

Write-Host "Adding $addedCount new 5-letter words"

# Build merged array
$merged5 = @($existing."5") + $newEntries

Write-Host "New 5-letter total: $($merged5.Count)"

$output = [ordered]@{
    "3" = $existing."3"
    "4" = $existing."4"
    "5" = $merged5
    "6" = $existing."6"
    "7" = $existing."7"
}

$json = $output | ConvertTo-Json -Depth 5
Set-Content -Path $wordsPath -Value $json -Encoding UTF8
Write-Host "words.json patched successfully!"

import os, glob, re
files = glob.glob('src/ui/frames/*.java')
for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    content = content.replace('this$0', 'this')
    content = re.sub(r'final\s+/\*\s*synthetic\s*\*/\s*[A-Za-z0-9_]+\s+this;\s*', '', content)
    content = re.sub(r'\{[^{}]*\.this\s*=[^{}]*\}', '', content)
    
    classname = os.path.basename(f).replace('.java', '')
    content = content.replace('this.this.', f'{classname}.this.')
    content = content.replace('this.this', f'{classname}.this')

    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)

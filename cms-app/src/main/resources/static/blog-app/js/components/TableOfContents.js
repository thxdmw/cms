// 直接移植原 home/fragments/catalogue.html 里的原生 JS 逻辑（本来就不是 jQuery 写的，改动很小）
function animateToggle(element, isExpanding) {
    if (element.classList.contains('animating')) return;
    element.classList.add('animating');
    if (isExpanding) {
        element.style.display = 'block';
        const fullHeight = element.scrollHeight + 'px';
        element.style.height = '0';
        element.offsetHeight;
        element.style.height = fullHeight;
    } else {
        element.style.height = element.scrollHeight + 'px';
        element.offsetHeight;
        element.style.height = '0';
    }
    element.addEventListener('transitionend', function handler() {
        if (isExpanding) {
            element.style.height = '';
            element.style.overflow = '';
        } else {
            element.style.display = 'none';
            element.style.height = '';
            element.style.overflow = '';
        }
        element.classList.remove('animating');
        element.removeEventListener('transitionend', handler);
    }, { once: true });
}

export default {
    setup(props, { expose }) {
        function build() {
            const content = document.querySelector('.article-body');
            const tocContainer = document.getElementById('toc');
            if (!content || !tocContainer) return;
            tocContainer.innerHTML = '';

            const headings = content.querySelectorAll('h1, h2, h3, h4, h5');
            let index = 0;
            const rootUl = document.createElement('ul');
            rootUl.style.listStyle = 'none';
            rootUl.style.paddingLeft = '0';
            tocContainer.appendChild(rootUl);

            const stack = [{ level: 0, ul: rootUl }];

            headings.forEach((heading) => {
                const level = parseInt(heading.tagName[1]);
                const id = `heading-${index++}`;
                heading.id = id;

                const li = document.createElement('li');
                li.dataset.level = level;

                const toggleBtn = document.createElement('span');
                toggleBtn.textContent = '\u{1F4DA}️';
                toggleBtn.style.cursor = 'pointer';
                toggleBtn.style.marginRight = '5px';
                toggleBtn.style.userSelect = 'none';

                const link = document.createElement('a');
                link.href = `#${id}`;
                link.textContent = heading.textContent;
                link.style.textDecoration = 'none';

                li.appendChild(toggleBtn);
                li.appendChild(link);

                while (level <= stack[stack.length - 1].level) stack.pop();
                const parentUl = stack[stack.length - 1].ul;
                parentUl.appendChild(li);

                const childUl = document.createElement('ul');
                childUl.style.listStyle = 'none';
                childUl.style.paddingLeft = '15px';
                childUl.style.display = 'none';
                childUl.style.overflow = 'hidden';
                li.appendChild(childUl);

                stack.push({ level, ul: childUl });

                toggleBtn.addEventListener('click', function () {
                    const isCurrentlyHidden = childUl.style.display === 'none' || childUl.style.height === '0px';
                    animateToggle(childUl, isCurrentlyHidden);
                });
            });

            const firstLevelLis = rootUl.querySelectorAll('li[data-level="1"]');
            firstLevelLis.forEach((li) => {
                const childUl = li.querySelector('ul');
                if (childUl) {
                    childUl.style.display = 'block';
                    childUl.style.height = '';
                }
            });

            const allLis = tocContainer.querySelectorAll('li');
            allLis.forEach((li) => {
                const childUl = li.querySelector('ul');
                const toggleBtn = li.querySelector('span');
                if (!childUl || childUl.children.length === 0) {
                    if (toggleBtn) toggleBtn.remove();
                    li.style.listStyleType = 'disc';
                    li.style.marginLeft = '20px';
                }
            });

            tocContainer.querySelectorAll('a').forEach((anchor) => {
                anchor.addEventListener('click', function (e) {
                    e.preventDefault();
                    const targetId = this.getAttribute('href').substring(1);
                    const target = document.getElementById(targetId);
                    const header = document.getElementById('header');
                    const headerHeight = header ? header.offsetHeight : 0;
                    const offsetTop = target.getBoundingClientRect().top + window.scrollY - headerHeight - 10;
                    window.scrollTo({ top: offsetTop, behavior: 'smooth' });
                    if (window.innerWidth <= 768) {
                        const tocContainerEl = document.querySelector('.article-toc-container');
                        if (tocContainerEl) tocContainerEl.style.display = 'none';
                    }
                });
            });
        }

        function setupMobileToggle() {
            const tocToggleBtn = document.getElementById('tocToggleBtn');
            const tocContainer = document.querySelector('.article-toc-container');
            if (!tocToggleBtn || !tocContainer) return;

            tocToggleBtn.onclick = function () {
                if (tocContainer.style.display === 'none' || tocContainer.style.display === '') {
                    tocContainer.style.display = 'block';
                } else {
                    tocContainer.style.display = 'none';
                }
            };

            if (window.innerWidth <= 768) tocContainer.style.display = 'none';

            window.addEventListener('resize', () => {
                tocContainer.style.display = window.innerWidth > 768 ? 'block' : 'none';
            });
        }

        expose({ build, setupMobileToggle });
        return {};
    },
    template: `
    <div class="article-toc-container hover-shadow">
        <div class="article-toc" style="border-radius:12px; margin-bottom:20px;">
            <h4 style="margin:10px;">目录</h4>
            <ul id="toc" class="toc-list" style="list-style:none; padding-left:0;"></ul>
        </div>
    </div>
    `
};

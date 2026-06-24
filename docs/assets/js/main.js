/* ============================================================
   main.js — PikaORM Docs
   1. Sidebar left toggle
   2. Nav section collapse/expand
   3. Right sidebar TOC builder + scroll spy
   4. Search modal (Lunr.js)
   ============================================================ */

(function () {
  'use strict';

  /* ── 1. SIDEBAR LEFT TOGGLE ─────────────────────────────── */
  const body          = document.body;
  const toggleBtn     = document.getElementById('sidebar-toggle');
  const COLLAPSED_KEY = 'pika-sidebar-collapsed';

  function setSidebarState(collapsed) {
    body.classList.toggle('sidebar-collapsed', collapsed);
    if (toggleBtn) {
      toggleBtn.setAttribute('aria-expanded', String(!collapsed));
    }
    try { localStorage.setItem(COLLAPSED_KEY, String(collapsed)); } catch (_) {}
  }

  // Restore persisted state
  (function initSidebar() {
    const isMobile = window.innerWidth <= 800;
    const stored   = localStorage.getItem(COLLAPSED_KEY);
    if (isMobile) {
      setSidebarState(true); // always start collapsed on mobile
    } else if (stored !== null) {
      setSidebarState(stored === 'true');
    }
  })();

  if (toggleBtn) {
    toggleBtn.addEventListener('click', function () {
      const isCollapsed = body.classList.contains('sidebar-collapsed');
      setSidebarState(!isCollapsed);
    });
  }

  // Close sidebar when clicking outside on mobile
  document.addEventListener('click', function (e) {
    if (window.innerWidth > 800) return;
    const sidebar = document.getElementById('sidebar-left');
    if (!sidebar) return;
    if (body.classList.contains('sidebar-collapsed')) return;
    if (!sidebar.contains(e.target) && e.target !== toggleBtn && !toggleBtn.contains(e.target)) {
      setSidebarState(true);
    }
  });


  /* ── 2. SIDEBAR RIGHT (TOC) TOGGLE ────────────────────── */
  const tocToggleBtn   = document.getElementById('toc-toggle');
  const TOC_COLLAPSED  = 'pika-toc-collapsed';

  function setTocState(collapsed) {
    body.classList.toggle('toc-collapsed', collapsed);
    if (tocToggleBtn) {
      tocToggleBtn.setAttribute('aria-expanded', String(!collapsed));
      tocToggleBtn.setAttribute('aria-label', collapsed ? 'Expand table of contents' : 'Collapse table of contents');
    }
    try { localStorage.setItem(TOC_COLLAPSED, String(collapsed)); } catch (_) {}
  }

  // Restore persisted state — default open (false = not collapsed)
  (function initToc() {
    const stored = localStorage.getItem(TOC_COLLAPSED);
    setTocState(stored === 'true'); // only collapse if explicitly stored as collapsed
  })();

  if (tocToggleBtn) {
    tocToggleBtn.addEventListener('click', function () {
      setTocState(!body.classList.contains('toc-collapsed'));
    });
  }


  /* ── 3. NAV SECTION COLLAPSE ────────────────────────────── */
  document.querySelectorAll('.nav-section-header').forEach(function (btn) {
    const targetId = btn.getAttribute('aria-controls');
    const target   = document.getElementById(targetId);
    const key      = 'pika-nav-' + btn.getAttribute('data-section-id');

    // Restore state
    const stored = localStorage.getItem(key);
    if (stored === 'false') {
      btn.setAttribute('aria-expanded', 'false');
      if (target) target.hidden = true;
    }

    btn.addEventListener('click', function () {
      const expanded = btn.getAttribute('aria-expanded') === 'true';
      btn.setAttribute('aria-expanded', String(!expanded));
      if (target) target.hidden = expanded;
      try { localStorage.setItem(key, String(!expanded)); } catch (_) {}
    });
  });


  /* ── 3. RIGHT TOC BUILDER + SCROLL SPY ─────────────────── */
  (function buildTOC() {
    const toc   = document.getElementById('toc');
    const prose = document.getElementById('main-content');
    if (!toc || !prose) return;

    const headings = Array.from(prose.querySelectorAll('h2, h3'));
    if (headings.length < 2) {
      // Hide right sidebar and its tab toggle if not enough headings
      const rightSidebar = document.getElementById('sidebar-right');
      const tocTab = document.getElementById('toc-toggle');
      if (rightSidebar) rightSidebar.style.display = 'none';
      if (tocTab) tocTab.style.display = 'none';
      document.querySelector('.layout-center') && (document.querySelector('.layout-center').style.marginRight = '0');
      return;
    }

    const fragment = document.createDocumentFragment();

    headings.forEach(function (h, i) {
      // Ensure heading has an id
      if (!h.id) {
        h.id = 'heading-' + i + '-' + h.textContent.trim()
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-|-$/g, '');
      }

      const a = document.createElement('a');
      a.href        = '#' + h.id;
      a.textContent = h.textContent.replace(/^[★▸]+\s*/, ''); // strip decorative prefixes
      a.className   = 'toc-link' + (h.tagName === 'H3' ? ' toc-h3' : '');
      a.setAttribute('data-target', h.id);

      a.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.getElementById(h.id);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      });

      fragment.appendChild(a);
    });

    toc.appendChild(fragment);

    // Scroll spy with IntersectionObserver
    const links = toc.querySelectorAll('a[data-target]');
    const headingMap = {};
    links.forEach(function (link) { headingMap[link.getAttribute('data-target')] = link; });

    let activeId = null;

    const observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          const id = entry.target.id;
          if (activeId !== id) {
            if (activeId && headingMap[activeId]) headingMap[activeId].classList.remove('toc-active');
            activeId = id;
            if (headingMap[id]) headingMap[id].classList.add('toc-active');
          }
        }
      });
    }, {
      rootMargin: '-60px 0px -70% 0px',
      threshold: 0
    });

    headings.forEach(function (h) { observer.observe(h); });
  })();


  /* ── 4. SEARCH MODAL ────────────────────────────────────── */
  (function initSearch() {
    const trigger   = document.getElementById('search-trigger');
    const modal     = document.getElementById('search-modal');
    const backdrop  = document.getElementById('search-modal-backdrop');
    const input     = document.getElementById('search-input');
    const results   = document.getElementById('search-results');
    const closeBtn  = document.getElementById('search-close');

    if (!modal || !trigger) return;

    let lunrIndex  = null;
    let pagesData  = [];
    let dataLoaded = false;
    let focusedIdx = -1;

    function openModal() {
      modal.hidden = false;
      trigger.setAttribute('aria-expanded', 'true');
      if (input) {
        input.focus();
        input.select();
      }
      document.body.style.overflow = 'hidden';
      if (!dataLoaded) loadSearchData();
    }

    function closeModal() {
      modal.hidden = true;
      trigger.setAttribute('aria-expanded', 'false');
      document.body.style.overflow = '';
      focusedIdx = -1;
    }

    // Open on button click
    trigger.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (backdrop) backdrop.addEventListener('click', closeModal);

    // Keyboard shortcut: ⌘K / Ctrl+K
    document.addEventListener('keydown', function (e) {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        modal.hidden ? openModal() : closeModal();
        return;
      }
      if (e.key === 'Escape' && !modal.hidden) {
        closeModal();
      }
    });

    // Arrow key navigation in results
    if (input) {
      input.addEventListener('keydown', function (e) {
        const items = results.querySelectorAll('.search-result-item');
        if (!items.length) return;
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          focusedIdx = Math.min(focusedIdx + 1, items.length - 1);
          updateFocus(items);
        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          focusedIdx = Math.max(focusedIdx - 1, 0);
          updateFocus(items);
        } else if (e.key === 'Enter') {
          const focused = items[focusedIdx];
          if (focused) { closeModal(); window.location = focused.href; }
        }
      });
    }

    function updateFocus(items) {
      items.forEach(function (item, i) {
        item.classList.toggle('search-result--focused', i === focusedIdx);
      });
      if (items[focusedIdx]) items[focusedIdx].scrollIntoView({ block: 'nearest' });
    }

    // Load search index JSON
    function loadSearchData() {
      const baseUrl = window.siteBaseUrl || '';
      fetch(baseUrl + '/search.json')
        .then(function (r) { return r.json(); })
        .then(function (data) {
          pagesData = data;
          dataLoaded = true;

          lunrIndex = lunr(function () {
            this.field('title',   { boost: 10 });
            this.field('content', { boost: 1 });
            this.field('category', { boost: 5 });
            this.ref('id');

            data.forEach(function (page, i) {
              this.add({
                id:       i,
                title:    page.title    || '',
                content:  page.content  || '',
                category: page.category || ''
              });
            }, this);
          });

          // Trigger a search if input already has text
          if (input && input.value.trim()) {
            runSearch(input.value.trim());
          }
        })
        .catch(function () {
          dataLoaded = true; // prevent repeated attempts
        });
    }

    // Search on input
    if (input) {
      input.addEventListener('input', function () {
        const q = input.value.trim();
        focusedIdx = -1;
        if (!q) {
          results.innerHTML = '<p class="search-hint">Type to search across all pages…</p>';
          return;
        }
        if (!dataLoaded) {
          results.innerHTML = '<p class="search-hint">Loading index…</p>';
          return;
        }
        runSearch(q);
      });
    }

    function runSearch(q) {
      if (!lunrIndex) {
        results.innerHTML = '<p class="search-hint">Search index not available.</p>';
        return;
      }

      let hits = [];
      try {
        hits = lunrIndex.search(q + '~1');
        if (!hits.length) hits = lunrIndex.search(q + '*');
        if (!hits.length) hits = lunrIndex.search(q);
      } catch (_) {
        try { hits = lunrIndex.search(q); } catch (_2) { hits = []; }
      }

      if (!hits.length) {
        results.innerHTML = '<p class="search-no-results">No results for "<strong>' + escHtml(q) + '</strong>"</p>';
        return;
      }

      // Build a regex to highlight matched tokens in accent color
      // Escape special regex chars in the query, split into words
      const terms = q.trim().split(/\s+/)
        .map(function (t) { return t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); })
        .filter(Boolean);
      const highlightRe = terms.length
        ? new RegExp('(' + terms.join('|') + ')', 'gi')
        : null;

      function highlight(rawText) {
        if (!rawText) return '';
        // Escape HTML first, then wrap matches
        const escaped = escHtml(rawText);
        if (!highlightRe) return escaped;
        return escaped.replace(highlightRe, '<mark class="search-highlight">$1</mark>');
      }

      const top = hits.slice(0, 8);
      const baseUrl = window.siteBaseUrl || '';
      const html = top.map(function (hit) {
        const page = pagesData[parseInt(hit.ref, 10)];
        if (!page) return '';
        // Excerpt: try to find a snippet containing the first matched term
        const rawContent = (page.content || '').replace(/\s+/g, ' ').trim();
        const excerpt    = findExcerpt(rawContent, q, 140);
        return '<a href="' + escHtml(baseUrl + page.url) + '" class="search-result-item">'
          + (page.category ? '<div class="search-result-category">' + escHtml(page.category) + '</div>' : '')
          + '<div class="search-result-title">'  + highlight(page.title || page.url) + '</div>'
          + (excerpt ? '<div class="search-result-excerpt">' + highlight(excerpt) + '\u2026</div>' : '')
          + '</a>';
      }).join('');

      results.innerHTML = html;
    }

    // Return a ~140-char excerpt centred on the first query term match
    function findExcerpt(text, q, maxLen) {
      if (!text) return '';
      const firstTerm = q.trim().split(/\s+/)[0];
      const idx = text.toLowerCase().indexOf(firstTerm.toLowerCase());
      if (idx === -1) return text.slice(0, maxLen);
      const half  = Math.floor(maxLen / 2);
      const start = Math.max(0, idx - half);
      const end   = Math.min(text.length, start + maxLen);
      return (start > 0 ? '\u2026' : '') + text.slice(start, end);
    }

    function escHtml(str) {
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
    }
  })();


  /* ── 4b. CODE-BLOCK CARDS (header + copy button) ────────── */
  (function enhanceCodeBlocks() {
    // Rouge emits: <div class="language-X highlighter-rouge">
    //                <div class="highlight"><pre class="highlight"><code>…
    // Mermaid blocks are <code class="language-mermaid"> and are converted
    // to .mermaid divs elsewhere — skip anything mermaid-related.
    var blocks = document.querySelectorAll('.prose .highlight, .hero-code-inner .highlight');
    if (!blocks.length) return;

    blocks.forEach(function (highlight) {
      // Guard: already wrapped, or no code to copy.
      if (highlight.closest('.code-card')) return;
      var code = highlight.querySelector('code');
      if (!code) return;

      // Skip mermaid (defensive — mermaid isn't wrapped in .highlight,
      // but bail if the source class somehow indicates a diagram).
      var outer = highlight.closest('[class*="language-"]') || highlight;
      if (/language-mermaid/.test(outer.className) ||
          /language-mermaid/.test(code.className)) {
        return;
      }

      // Derive a label from the language-* class on the outer wrapper.
      var label = 'CODE';
      var m = (outer.className || '').match(/language-([a-z0-9+#-]+)/i);
      if (m && m[1] && m[1] !== 'plaintext' && m[1] !== 'text') {
        label = m[1].toUpperCase();
      }

      // Build the card frame.
      var card = document.createElement('div');
      card.className = 'code-card';

      var header = document.createElement('div');
      header.className = 'code-card-header';
      header.innerHTML =
        '<span class="code-card-dot" aria-hidden="true"></span>' +
        '<span class="code-card-label"></span>';
      header.querySelector('.code-card-label').textContent = label;

      var footer = document.createElement('div');
      footer.className = 'code-card-footer';

      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'code-copy-btn';
      btn.setAttribute('aria-label', 'Copy code to clipboard');
      btn.textContent = 'Copy';

      footer.appendChild(btn);

      // Insert the card before the highlight, then move pieces inside.
      var parent = outer.parentNode;
      if (!parent) return;
      parent.insertBefore(card, outer);
      card.appendChild(header);
      card.appendChild(highlight);
      card.appendChild(footer);

      // The outer .language-* wrapper is now empty; remove it if so.
      if (outer !== highlight && !outer.children.length) {
        outer.remove();
      }

      var resetTimer = null;
      btn.addEventListener('click', function () {
        var text = code.innerText;
        var done = function () {
          btn.textContent = 'Copied!';
          btn.classList.add('is-copied');
          if (resetTimer) clearTimeout(resetTimer);
          resetTimer = setTimeout(function () {
            btn.textContent = 'Copy';
            btn.classList.remove('is-copied');
          }, 1600);
        };

        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(text).then(done).catch(fallbackCopy);
        } else {
          fallbackCopy();
        }

        function fallbackCopy() {
          try {
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
            done();
          } catch (_) {
            btn.textContent = 'Error';
            if (resetTimer) clearTimeout(resetTimer);
            resetTimer = setTimeout(function () { btn.textContent = 'Copy'; }, 1600);
          }
        }
      });
    });
  })();


  /* ── 5. GITHUB ALERTS ───────────────────────────────────── */
  (function initAlerts() {
    var bqs = document.querySelectorAll('.prose blockquote');
    bqs.forEach(function (bq) {
      var p = bq.querySelector('p:first-child');
      if (!p) return;
      var text = p.textContent.trim();
      var match = text.match(/^\[!(NOTE|IMPORTANT|WARNING|CAUTION|TIP)\]/i);
      if (match) {
        var type = match[1].toLowerCase();
        bq.classList.add('alert', 'alert-' + type);
        
        // Remove the [!TYPE] text but keep the rest of the node
        var firstNode = p.childNodes[0];
        if (firstNode && firstNode.nodeType === Node.TEXT_NODE) {
          firstNode.textContent = firstNode.textContent.replace(/^\[!.*?\]\s*/i, '');
        }
      }
    });
  })();

  /* ── 5. MERMAID DIAGRAM ZOOM LIGHTBOX ─────────────────── */
  (function initDiagramZoom() {

    // Build the modal once and append to body
    var dmModal = document.createElement('div');
    dmModal.id        = 'diagram-modal';
    dmModal.className = 'diagram-modal';
    dmModal.setAttribute('role', 'dialog');
    dmModal.setAttribute('aria-modal', 'true');
    dmModal.setAttribute('aria-label', 'Diagram viewer');
    dmModal.hidden = true;
    dmModal.innerHTML =
      '<div class="diagram-modal-backdrop" id="dm-backdrop"></div>' +
      '<div class="diagram-modal-box">' +
        '<div class="diagram-toolbar">' +
          '<span class="diagram-hint">Scroll to zoom &nbsp;·&nbsp; Drag to pan &nbsp;·&nbsp; Double-click to reset</span>' +
          '<button class="diagram-close" id="dm-close">Close &nbsp;✕</button>' +
        '</div>' +
        '<div class="diagram-canvas" id="dm-canvas"></div>' +
      '</div>';
    document.body.appendChild(dmModal);

    var dmCanvas   = document.getElementById('dm-canvas');
    var dmClose    = document.getElementById('dm-close');
    var dmBackdrop = document.getElementById('dm-backdrop');

    var scale = 1, tx = 0, ty = 0;
    var dragging = false, dragStartX = 0, dragStartY = 0, dragTx = 0, dragTy = 0;
    var activeSvg = null;

    function applyTransform() {
      if (activeSvg) {
        activeSvg.style.transform =
          'translate(' + tx + 'px, ' + ty + 'px) scale(' + scale + ')';
      }
    }

    function openDiagram(sourceSvg) {
      // Clone the SVG so we don't mutate the page version
      var clone = sourceSvg.cloneNode(true);
      clone.removeAttribute('width');
      clone.removeAttribute('height');
      clone.style.width        = 'auto';
      clone.style.height       = 'auto';
      clone.style.maxWidth     = 'none';
      clone.style.display      = 'block';
      clone.style.transformOrigin = 'center center';

      dmCanvas.innerHTML = '';
      dmCanvas.appendChild(clone);
      activeSvg = clone;

      scale = 1; tx = 0; ty = 0;
      applyTransform();

      dmModal.hidden = false;
      document.body.style.overflow = 'hidden';
      dmClose.focus();
    }

    function closeDiagram() {
      dmModal.hidden = true;
      document.body.style.overflow = '';
      activeSvg = null;
    }

    dmClose.addEventListener('click', closeDiagram);
    dmBackdrop.addEventListener('click', closeDiagram);
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && !dmModal.hidden) closeDiagram();
    });

    // ── Scroll to zoom ──────────────────────────────────────
    dmCanvas.addEventListener('wheel', function (e) {
      e.preventDefault();
      var factor = e.deltaY < 0 ? 1.12 : (1 / 1.12);
      scale = Math.min(Math.max(scale * factor, 0.15), 10);
      applyTransform();
    }, { passive: false });

    // ── Drag to pan ─────────────────────────────────────────
    dmCanvas.addEventListener('mousedown', function (e) {
      if (e.button !== 0) return;
      dragging  = true;
      dragStartX = e.clientX; dragStartY = e.clientY;
      dragTx = tx; dragTy = ty;
    });
    window.addEventListener('mousemove', function (e) {
      if (!dragging) return;
      tx = dragTx + (e.clientX - dragStartX);
      ty = dragTy + (e.clientY - dragStartY);
      applyTransform();
    });
    window.addEventListener('mouseup', function () { dragging = false; });

    // ── Double-click to reset ───────────────────────────────
    dmCanvas.addEventListener('dblclick', function () {
      scale = 1; tx = 0; ty = 0;
      applyTransform();
    });

    // ── Touch support ────────────────────────────────────────
    var lastTouchDist = 0;
    var touchStartTx = 0, touchStartTy = 0, touchStartX = 0, touchStartY = 0;

    dmCanvas.addEventListener('touchstart', function (e) {
      if (e.touches.length === 2) {
        lastTouchDist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        );
      } else if (e.touches.length === 1) {
        touchStartX  = e.touches[0].clientX;
        touchStartY  = e.touches[0].clientY;
        touchStartTx = tx; touchStartTy = ty;
      }
    }, { passive: true });

    dmCanvas.addEventListener('touchmove', function (e) {
      e.preventDefault();
      if (e.touches.length === 2) {
        var dist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        );
        if (lastTouchDist > 0) {
          scale = Math.min(Math.max(scale * (dist / lastTouchDist), 0.15), 10);
          applyTransform();
        }
        lastTouchDist = dist;
      } else if (e.touches.length === 1) {
        tx = touchStartTx + (e.touches[0].clientX - touchStartX);
        ty = touchStartTy + (e.touches[0].clientY - touchStartY);
        applyTransform();
      }
    }, { passive: false });

    dmCanvas.addEventListener('touchend', function () { lastTouchDist = 0; });

    // ── Attach click handlers to .mermaid divs ───────────────
    // Mermaid renders SVG async, so we watch via MutationObserver
    function attachZoom(mermaidDiv) {
      if (mermaidDiv.dataset.zoomReady) return;
      mermaidDiv.dataset.zoomReady = '1';

      mermaidDiv.addEventListener('click', function () {
        var svg = mermaidDiv.querySelector('svg');
        if (svg) openDiagram(svg);
      });
    }

    // Watch for SVGs injected by Mermaid after load
    var mo = new MutationObserver(function (mutations) {
      mutations.forEach(function (m) {
        m.addedNodes.forEach(function (node) {
          if (node.nodeType !== 1) return;
          if (node.tagName === 'svg') {
            var parent = node.parentElement;
            if (parent && parent.classList.contains('mermaid')) {
              attachZoom(parent);
            }
          }
          // Also scan inside added subtrees
          node.querySelectorAll && node.querySelectorAll('.mermaid').forEach(attachZoom);
        });
      });
    });
    mo.observe(document.body, { childList: true, subtree: true });

    // Attach to any already-rendered diagrams immediately
    document.querySelectorAll('.mermaid').forEach(attachZoom);

  })();

})();

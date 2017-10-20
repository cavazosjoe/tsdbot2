<#-- @ftlvariable name="" type="org.tsd.tsdbot.view.TSDTVView" -->
<#import "layout.ftl" as layout>
<@layout.layout title="TSDTV">

    <script type="text/javascript">

        (function refresh_now_playing() {
            $.get('/tsdtv/nowPlaying',{},function(responseJson) {

                var nowPlayingSeries = null;
                var nowPlayingSeason = null;
                var nowPlayingName = null;

                if (responseJson.nowPlaying) {
                    if (responseJson.nowPlaying.media.seriesName) {
                        nowPlayingSeries = responseJson.nowPlaying.media.seriesName;
                    }
                    if (responseJson.nowPlaying.media.seasonName) {
                        nowPlayingSeason = responseJson.nowPlaying.media.seasonName;
                    }
                    nowPlayingName = responseJson.nowPlaying.media.seriesName;
                }

                var nowPlayingBody = (!nowPlayingName) ?
                    '<em>Nothing Playing</em>'
                    : '<h4>'+nowPlayingSeries+( (nowPlayingSeason) ? ' ('+nowPlayingSeason+')' : '' )+'</h4>' +
                        ''+nowPlayingName;

                var htmlString =
                    '<div class="panel panel-default">' +
                    '   <div class="panel-heading">' +
                    '       <h3 class="panel-title" style="color: #00bc7e"><strong>NOW PLAYING</strong></h3>' +
                    '   </div>' +
                    '   <div class="panel-body">' +
                    '       <div id="nowplaying">' +
                    '           <div class="media">' +
                    '               <div class="media-left media-middle">' +
                    '                   <img class="media-object" src="https://i.imgur.com/OXQrvmy.jpg"/>' +
                    '               </div>' +
                    '               <div class="media-body">' +
                    '                   '+ nowPlayingBody +
                    '               </div>' +
                    '           </div>' +
                    '       </div>' +
                    '   </div>' +
                    '</div>';

                $("#schedule").html(htmlString).text();

            }).then(function() {
                setTimeout(refresh_now_playing, 1000 * 5);
            });
        })();

        (function refresh_catalog() {
            $.get('/tsdtv/listings',{},function(responseJson) {

                var htmlString =
                    '<div class="panel panel-default">' +
                        '<div id="show1-header" class="panel-heading" role="tab">' +
                            '<h4 class="panel-title">' +
                                '<a class="collapsed" data-toggle="collapse" data-parent="#catalog" href="#show1-content" aria-expanded="true" aria-controls="show1-content">Movies</a>' +
                            '</h4>' +
                        '</div>' +
                        '<div id="show1-content" class="panel-collapse collapse" role="tabpanel" aria-labelledby="show1-header">' +
                            '<div class="panel-body">' +
                                '<ul class="list-group">';

                $.each(responseJson.allMovies, function(i, item) {
                    htmlString +=
                        '<li class="list-group-item">' +
                            item.media.name +
                            '<button class="play" agentId="'+item.agentId+'" mediaId="'+item.media.id+'">Play</button>' +
                        '</li>';
                });

                htmlString +=
                                '</ul>' +
                            '</div>' +
                        '</div>' +
                    '</div>';

                $("#catalog").html(htmlString).text();

            }).then(function() {
                setTimeout(refresh_catalog, 1000 * 60 * 5);
            });
        })();

        $(function() {
            $(document).on("click", ".play", function() {
                agentId = $(this).attr('agentId');
                mediaId = $(this).attr('mediaId');
                console.log('agentId: '+agentId+', mediaId:'+mediaId);
                var json = {};
                json['agentId'] = agentId;
                json['mediaId'] = mediaId;
                $.ajax({
                    type: "POST",
                    url: "/tsdtv/play",
                    data: JSON.stringify(json),
                    contentType:"application/json; charset=utf-8",
                    success: function (msg) {
                        $.bootstrapGrowl('Your video will begin shortly', {
                            type: 'info',
                            width: 'auto',
                            allow_dismiss: true
                        });
                    },
                    error: function(xhr, status, error) {
                        $.bootstrapGrowl(xhr.responseText, {
                            type: 'danger',
                            width: 'auto',
                            allow_dismiss: true
                        });
                    }
                });
                event.preventDefault();
            });
        });

    </script>

    <div class="col-md-9">
        <#if playerType == "vlc">
            <embed autoplay="yes" target="${streamUrl}"
                   loop="true" name="VLC"
                   type="application/x-vlc-plugin" volume="100" height="750" width="100%" id="vlc">
                <a href="/tsdtv">videojs version</a>
        <#elseif playerType == "videojs">
            <video class="video-js vjs-default-skin"
                   style="width: 100%; height: 750px;"
                   controls loop preload="auto"
                   poster="http://i.imgur.com/4Q7jsCr.jpg"
                   data-setup='{"autoplay": true, "techorder": ["flash","html5"]}'>
                <source src="${streamUrl?html}" type='rtmp/mp4' />
                <p class="vjs-no-js">
                    To view this video please enable JavaScript, and consider upgrading to a web browser that
                    <a href="http://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a>
                </p>
            </video>
            <a href="/tsdtv?playerType=vlc">VLC version</a>
        </#if>
    </div>
    <div class="col-md-3">
        <div role="tabpanel">

            <!--Tabs-->
            <ul class="nav nav-tabs nav-justified" role="tablist">
                <li role="presentation" class="active">
                    <a href="#schedule" aria-controls="schedule" role="tab" data-toggle="tab">Schedule</a>
                </li>
                <li role="presentation">
                    <a href="#catalogTab" aria-controls="catalogTab" role="tab" data-toggle="tab">Catalog</a>
                </li>
                <li role="presentation">
                    <a href="#chat" aria-controls="chat" role="tab" data-toggle="tab">Chat</a>
                </li>
            </ul>

            <!--Tab content-->
            <div class="tab-content">

                <!--Schedule tab-->
                <div id="schedule" role="tabpanel" class="tab-pane active">

                    <!--Now playing-->
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h3 class="panel-title" style="color: #00bc7e"><strong>NOW PLAYING</strong></h3>
                        </div>
                        <div class="panel-body">
                            <div id="nowplaying">Loading...</div>
                        </div>
                    </div>

                    <!--Next in queue-->
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h3 class="panel-title" style="color: #00bc7e">Up next</h3>
                        </div>
                        <div class="panel-body">
                            <div id="queue">Loading...</div>
                        </div>
                    </div>

                    <!--Scheduled blocks-->
                    <div id="blocks">Loading...</div>
                </div>

                <!--Catalog tab-->
                <div role="tabpanel" class="tab-pane" id="catalogTab" style="max-height: 700px; overflow-y: scroll;">
                    <div class="panel-group" id="catalog" role="tablist" aria-multiselectable="true">

                        <!--Show One-->
                        <div class="panel panel-default">
                            <div id="show1-header" class="panel-heading" role="tab">
                                <h4 class="panel-title">
                                    <a class="collapsed" data-toggle="collapse" data-parent="#catalog" href="#show1-content" aria-expanded="true" aria-controls="show1-content">Show One</a>
                                </h4>
                            </div>
                            <div id="show1-content" class="panel-collapse collapse" role="tabpanel" aria-labelledby="show1-header">
                                <div class="panel-body">
                                    <ul class="list-group">
                                        <li class="list-group-item">
                                            Episode 1
                                            <input class="btn btn-sm btn-success" type="submit" value="Play"/>
                                        </li>
                                        <li class="list-group-item">
                                            Episode 2
                                            <input class="btn btn-sm btn-success" type="submit" value="Play"/>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>

                        <!--Show Two-->
                        <div class="panel panel-default">
                            <div id="show2-header" class="panel-heading" role="tab">
                                <h4 class="panel-title">
                                    <a data-toggle="collapse" data-parent="#catalog" href="#show2-content" aria-expanded="true" aria-controls="show2-content">Show Two</a>
                                </h4>
                            </div>
                            <div id="show2-content" class="panel-collapse collapse" role="tabpanel" aria-labelledby="show2-header">
                                <div class="panel-body">
                                    <ul class="list-group">
                                        <li class="list-group-item">
                                            Episode 1
                                            <input class="btn btn-sm btn-success" type="submit" value="Play"/>
                                        </li>
                                        <li class="list-group-item">
                                            Episode 2
                                            <input class="btn btn-sm btn-success" type="submit" value="Play"/>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!--Chat tab-->
                <div role="tabpanel" class="tab-pane" id="chat">
                    <iframe src="https://discordapp.com/widget?id=355036967022362635&theme=dark" width="350" height="500" allowtransparency="true" frameborder="0"></iframe>
                </div>

            </div>
        </div>
    </div>
</@layout.layout>